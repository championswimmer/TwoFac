#!/usr/bin/env node

import path from "node:path";
import {
  exists,
  markdownTable,
  parseSimpleArgs,
  readText,
  repoRoot,
  runCommand,
  splitCsv,
} from "./dependencyUpdateTools.mjs";

const PROJECTS = [
  "website",
  "composeApp/src/wasmJsMain/typescript",
];

async function projectDirs(root, selected) {
  const selectedSet = new Set(selected);
  const results = [];

  for (const relativePath of PROJECTS) {
    if (selectedSet.size > 0 && !selectedSet.has(relativePath)) continue;
    const projectDir = path.join(root, relativePath);
    if (await exists(path.join(projectDir, "package.json"))) {
      results.push(projectDir);
    }
  }

  return results;
}

async function runNpmOutdated(projectDir) {
  return runCommand("npm", ["outdated", "--json"], {
    cwd: projectDir,
    allowFailure: true,
    timeoutMs: 120_000,
    maxBuffer: 25 * 1024 * 1024,
  });
}

async function loadPackageTypes(projectDir) {
  const data = JSON.parse(await readText(path.join(projectDir, "package.json")));
  const mapping = new Map();

  for (const packageName of Object.keys(data.dependencies ?? {})) {
    mapping.set(packageName, "dependency");
  }
  for (const packageName of Object.keys(data.devDependencies ?? {})) {
    mapping.set(packageName, "devDependency");
  }
  for (const packageName of Object.keys(data.optionalDependencies ?? {})) {
    mapping.set(packageName, "optionalDependency");
  }

  return mapping;
}

function parseOutdated(stdout) {
  if (!stdout?.trim()) return {};
  const parsed = JSON.parse(stdout);
  return Array.isArray(parsed) ? {} : parsed;
}

async function renderReport(root, selected) {
  const dirs = await projectDirs(root, selected);
  if (dirs.length === 0) {
    return "## npm dependency surface\n\nNo npm projects were discovered.\n";
  }

  const rows = [["Project", "Package", "Type", "Current", "Wanted", "Latest"]];
  const notes = [];

  for (const directory of dirs) {
    const result = await runNpmOutdated(directory);
    const packageTypes = await loadPackageTypes(directory);
    const data = parseOutdated(result.stdout);
    const relativePath = path.relative(root, directory);

    if (result.stderr && !result.stdout) {
      notes.push(`- \`${relativePath}\`: \`${result.stderr.trim()}\``);
    }

    for (const packageName of Object.keys(data).sort()) {
      const meta = data[packageName] ?? {};
      rows.push([
        relativePath,
        packageName,
        packageTypes.get(packageName) ?? "dependency",
        String(meta.current ?? "—"),
        String(meta.wanted ?? "—"),
        String(meta.latest ?? "—"),
      ]);
    }

    if (result.code === 0 && Object.keys(data).length === 0) {
      notes.push(`- \`${relativePath}\`: no outdated direct dependencies`);
    }
  }

  const body = rows.length > 1 ? markdownTable(rows) : "No outdated npm dependencies were reported.";
  const notesBlock = notes.length > 0 ? `\n${notes.join("\n")}` : "";

  return `## npm dependency surface

- npm projects in this repo: \`website\`, \`composeApp/src/wasmJsMain/typescript\`
- Audit command per project: \`npm outdated --json\`

### Direct dependency update candidates

${body}
${notesBlock}
`;
}

async function runProjectCommand(command, args, cwd) {
  await runCommand(command, args, {
    cwd,
    timeoutMs: 300_000,
    maxBuffer: 100 * 1024 * 1024,
  });
}

async function upgradeWanted(projectDir) {
  await runProjectCommand("npm", ["update"], projectDir);
  return `- \`${projectDir}\`: ran \`npm update\``;
}

async function upgradeLatest(projectDir) {
  const packageTypes = await loadPackageTypes(projectDir);
  const result = await runNpmOutdated(projectDir);
  const outdated = parseOutdated(result.stdout);
  const grouped = {
    dependency: [],
    devDependency: [],
    optionalDependency: [],
  };

  for (const packageName of Object.keys(outdated).sort()) {
    const dependencyType = packageTypes.get(packageName) ?? "dependency";
    grouped[dependencyType] ??= [];
    grouped[dependencyType].push(packageName);
  }

  const notes = [];
  if (grouped.dependency.length > 0) {
    await runProjectCommand("npm", ["install", ...grouped.dependency.map((name) => `${name}@latest`)], projectDir);
    notes.push(`- \`${projectDir}\`: upgraded dependencies to latest -> ${grouped.dependency.join(", ")}`);
  }
  if (grouped.devDependency.length > 0) {
    await runProjectCommand("npm", ["install", "-D", ...grouped.devDependency.map((name) => `${name}@latest`)], projectDir);
    notes.push(`- \`${projectDir}\`: upgraded devDependencies to latest -> ${grouped.devDependency.join(", ")}`);
  }
  if (grouped.optionalDependency.length > 0) {
    await runProjectCommand("npm", ["install", "-O", ...grouped.optionalDependency.map((name) => `${name}@latest`)], projectDir);
    notes.push(`- \`${projectDir}\`: upgraded optionalDependencies to latest -> ${grouped.optionalDependency.join(", ")}`);
  }
  if (notes.length === 0) {
    notes.push(`- \`${projectDir}\`: no direct outdated packages to upgrade`);
  }
  return notes;
}

async function renderUpgrade(root, selected, strategy) {
  const dirs = await projectDirs(root, selected);
  if (dirs.length === 0) {
    return "## npm upgrade result\n\nNo npm projects were discovered.\n";
  }

  const notes = ["## npm upgrade result", "", `Strategy: \`${strategy}\``, ""];
  for (const directory of dirs) {
    if (strategy === "wanted") {
      notes.push(await upgradeWanted(directory));
    } else {
      notes.push(...(await upgradeLatest(directory)));
    }
  }
  return `${notes.join("\n")}\n`;
}

function printHelp() {
  console.log(`npm dependency audit/upgrade helper

Usage:
  node .agents/skills/dependency-updates/scripts/npm-updates.mjs [mode] [options]

Modes:
  report   Audit direct dependencies with npm outdated (default)
  upgrade  Apply wanted or latest updates

Options:
  --strategy <wanted|latest>  Upgrade strategy for upgrade mode
  --projects <csv>            Limit processing to selected project paths
  --help                      Show this help
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

  const strategy = parsed.strategy ?? "wanted";
  if (!["wanted", "latest"].includes(strategy)) {
    throw new Error(`Unknown npm strategy: ${strategy}`);
  }

  const root = repoRoot();
  const selected = splitCsv(parsed.projects);

  if (mode === "report") {
    process.stdout.write(await renderReport(root, selected));
    return;
  }

  process.stdout.write(await renderUpgrade(root, selected, strategy));
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
