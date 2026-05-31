#!/usr/bin/env node

import { readdir } from "node:fs/promises";
import path from "node:path";
import {
  exists,
  parseSimpleArgs,
  repoRoot,
  runCommand,
  splitCsv,
} from "./dependencyUpdateTools.mjs";

async function collectPodfiles(root, current = root, results = []) {
  const entries = await readdir(current, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(current, entry.name);
    if (entry.isDirectory()) {
      if (["Pods", "build", "node_modules", ".git", ".gradle"].includes(entry.name)) continue;
      await collectPodfiles(root, fullPath, results);
      continue;
    }
    if (entry.isFile() && entry.name === "Podfile") {
      results.push(path.dirname(fullPath));
    }
  }
  return results.sort();
}

async function podCommand(projectDir) {
  return (await exists(path.join(projectDir, "Gemfile"))) ? ["bundle", "exec", "pod"] : ["pod"];
}

async function renderReport(root) {
  const dirs = await collectPodfiles(root);
  if (dirs.length === 0) {
    return `## CocoaPods dependency surface

- No \`Podfile\` was found in this repository.
- \`iosApp\` currently uses Gradle-built \`TwoFacUIKit\` / \`TwoFacKit\` frameworks instead of CocoaPods-managed dependencies.
- Keep this script so future native iOS pods can be audited with \`pod outdated --no-repo-update\`.
`;
  }

  const sections = [
    "## CocoaPods dependency surface",
    "",
    "- Audit command per pod project: `pod outdated --no-repo-update`",
  ];

  for (const directory of dirs) {
    const relativePath = path.relative(root, directory);
    if (!(await exists(path.join(directory, "Podfile.lock")))) {
      sections.push(
        "",
        `### \`${relativePath}\``,
        "",
        "`Podfile.lock` is missing, so `pod outdated` cannot compare installed versions yet.",
      );
      continue;
    }

    const command = [...(await podCommand(directory)), "outdated", "--no-repo-update"];
    const result = await runCommand(command[0], command.slice(1), {
      cwd: directory,
      allowFailure: true,
      timeoutMs: 180_000,
      maxBuffer: 25 * 1024 * 1024,
    });
    const output = (result.stdout || result.stderr || "No CocoaPods output was produced.").trim();
    sections.push(
      "",
      `### \`${relativePath}\``,
      "",
      "```text",
      output,
      "```",
    );
  }

  return `${sections.join("\n")}\n`;
}

async function renderUpgrade(root, options) {
  const dirs = await collectPodfiles(root);
  if (dirs.length === 0) {
    return "## CocoaPods upgrade result\n\nNo `Podfile` was found, so no CocoaPods upgrades were applied.\n";
  }

  const notes = ["## CocoaPods upgrade result", ""];
  for (const directory of dirs) {
    const command = [...(await podCommand(directory)), "update", ...options.pods];
    if (options.repoUpdate) command.push("--repo-update");
    await runCommand(command[0], command.slice(1), {
      cwd: directory,
      timeoutMs: 600_000,
      maxBuffer: 100 * 1024 * 1024,
    });
    notes.push(`- \`${path.relative(root, directory)}\`: ran \`${command.join(" ")}\``);
  }
  return `${notes.join("\n")}\n`;
}

function printHelp() {
  console.log(`CocoaPods dependency audit/upgrade helper

Usage:
  node .agents/skills/dependency-updates/scripts/cocoapods-updates.mjs [mode] [options]

Modes:
  report   Audit pod projects with pod outdated (default)
  upgrade  Run pod update for discovered Podfiles

Options:
  --repo-update   Pass --repo-update during pod update
  --pods <csv>    Limit pod update to specific pod names
  --help          Show this help
`);
}

async function main() {
  const parsed = parseSimpleArgs(process.argv.slice(2));
  if (parsed.help) {
    printHelp();
    return;
  }

  const mode = parsed._[0] ?? "report";
  if (!["report", "upgrade"].includes(mode)) {
    throw new Error(`Unknown mode: ${mode}`);
  }

  const root = repoRoot();
  if (mode === "report") {
    process.stdout.write(await renderReport(root));
    return;
  }

  process.stdout.write(await renderUpgrade(root, {
    repoUpdate: Boolean(parsed["repo-update"]),
    pods: splitCsv(parsed.pods),
  }));
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
