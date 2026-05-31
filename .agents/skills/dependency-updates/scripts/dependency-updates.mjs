#!/usr/bin/env node

import path from "node:path";
import {
  ensureDir,
  formatTimestamp,
  parseSimpleArgs,
  repoRoot,
  runCommand,
  splitCsv,
  writeText,
} from "./dependencyUpdateTools.mjs";

const HELPERS = {
  gradle: "gradle-updates.mjs",
  npm: "npm-updates.mjs",
  cocoapods: "cocoapods-updates.mjs",
};

async function runHelper(root, scriptName, args) {
  const scriptPath = path.join(root, ".agents", "skills", "dependency-updates", "scripts", scriptName);
  const result = await runCommand("node", [scriptPath, ...args], {
    cwd: root,
    timeoutMs: 600_000,
    maxBuffer: 100 * 1024 * 1024,
  });
  return result.stdout.trim();
}

async function discoverSurfaceSummary(root) {
  const npmProjects = [
    "website",
    "composeApp/src/wasmJsMain/typescript",
  ];

  const podfileSearch = await runCommand("find", [root, "-name", "Podfile", "-not", "-path", "*/Pods/*", "-not", "-path", "*/build/*", "-not", "-path", "*/node_modules/*"], {
    allowFailure: true,
    timeoutMs: 60_000,
  });
  const hasPodfiles = podfileSearch.stdout.split(/\r?\n/).map((line) => line.trim()).filter(Boolean).length > 0;

  return `## Repository dependency surfaces

- **Gradle / Kotlin Multiplatform:** \`sharedLib\`, \`composeApp\`, \`cliApp\`, \`androidApp\`, \`watchApp\` share \`gradle/libs.versions.toml\`
- **npm:** ${npmProjects.map((project) => `\`${project}\``).join(", ")}
- **Apple native wrappers:** \`iosApp\` contains Xcode project/workspace files but currently no \`Podfile\`, \`Podfile.lock\`, \`Package.swift\`, or \`Package.resolved\`
- **Ignore for dependency audits:** \`kotlin-js-store/*.yarn.lock\` exist without nearby \`package.json\`, so they are not first-class source package manifests
- **CocoaPods detected now:** ${hasPodfiles ? "yes" : "no"}
`;
}

function reportPath(root, output) {
  if (output) {
    return path.isAbsolute(output) ? output : path.join(root, output);
  }
  return path.join(root, ".agents", "plans", "dependency-update-reports", `dependency-update-report-${formatTimestamp()}.md`);
}

function formatLocalDateTime(date = new Date(), separator = "T") {
  const pad = (value) => String(value).padStart(2, "0");
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join("-") + `${separator}${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

async function renderReport(root, options) {
  const now = new Date();
  const header = [
    "---",
    `name: Dependency Update Report ${formatLocalDateTime(now, " ")}`,
    "status: Completed",
    "generated_by: dependency-updates skill",
    "---",
    "",
    "# Dependency Update Report",
    "",
    `Generated on \`${formatLocalDateTime(now)}\`.`,
  ].join("\n");

  const sections = [header, await discoverSurfaceSummary(root)];

  if (!options.skipGradle) {
    const args = ["report"];
    if (options.allowPrerelease) args.push("--allow-prerelease");
    sections.push(await runHelper(root, HELPERS.gradle, args));
  }
  if (!options.skipNpm) {
    const args = ["report"];
    if (options.projects.length > 0) args.push("--projects", options.projects.join(","));
    sections.push(await runHelper(root, HELPERS.npm, args));
  }
  if (!options.skipCocoapods) {
    sections.push(await runHelper(root, HELPERS.cocoapods, ["report"]));
  }

  sections.push(`## Validation commands after any future upgrade

- Root baseline: \`./gradlew check\`
- Shared/core regression: \`./gradlew :sharedLib:allTests :cliApp:allTests\`
- Compose/Desktop/Web regression: \`./gradlew :composeApp:desktopTest :composeApp:wasmJsBrowserTest :composeApp:checkXcodeProjectConfiguration\`
- Android wrappers: \`./gradlew :androidApp:testDebugUnitTest :watchApp:testDebugUnitTest\`
- Apple framework smoke build: \`./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :sharedLib:linkDebugFrameworkIosSimulatorArm64\`
- Website build: \`cd website && npm run build\`
- Wasm TypeScript interop build: \`cd composeApp/src/wasmJsMain/typescript && npm run build\`
- Optional UI/E2E follow-up: see \`.agents/skills/ui-testing/SKILL.md\`
`);

  const outputPath = reportPath(root, options.output);
  await ensureDir(path.dirname(outputPath));
  await writeText(outputPath, `${sections.map((section) => section.trim()).filter(Boolean).join("\n\n")}\n`);
  return outputPath;
}

async function performUpgrades(root, options) {
  const sections = [];

  if (!options.skipGradle) {
    const args = ["upgrade"];
    if (options.allowPrerelease) args.push("--allow-prerelease");
    if (options.gradleKeys.length > 0) args.push("--keys", options.gradleKeys.join(","));
    sections.push(await runHelper(root, HELPERS.gradle, args));
  }
  if (!options.skipNpm) {
    const args = ["upgrade", "--strategy", options.npmStrategy];
    if (options.projects.length > 0) args.push("--projects", options.projects.join(","));
    sections.push(await runHelper(root, HELPERS.npm, args));
  }
  if (!options.skipCocoapods) {
    const args = ["upgrade"];
    if (options.cocoapodsRepoUpdate) args.push("--repo-update");
    if (options.cocoapodsPods.length > 0) args.push("--pods", options.cocoapodsPods.join(","));
    sections.push(await runHelper(root, HELPERS.cocoapods, args));
  }

  process.stdout.write(`${sections.map((section) => section.trim()).filter(Boolean).join("\n\n")}\n\nNext: run the validation checklist in \`.agents/skills/dependency-updates/UPGRADING.md\`.\n`);
}

function printHelp() {
  console.log(`Dependency update report / upgrade helper

Usage:
  node .agents/skills/dependency-updates/scripts/dependency-updates.mjs [mode] [options]

Modes:
  report   Generate markdown report in .agents/plans/dependency-update-reports (default)
  upgrade  Apply upgrades only when explicitly requested

Policy:
  - Default upgrade intent is latest stable versions only
  - Do not use alpha/beta/rc/pre-release versions unless the user explicitly asks or approves
  - For Gradle pre-releases, this requires --allow-prerelease

Options:
  --skip-gradle
  --skip-npm
  --skip-cocoapods
  --allow-prerelease
  --gradle-keys <csv>
  --npm-strategy <wanted|latest>
  --projects <csv>
  --cocoapods-pods <csv>
  --cocoapods-repo-update
  --output <path>
  --help
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

  const npmStrategy = parsed["npm-strategy"] ?? "wanted";
  if (!["wanted", "latest"].includes(npmStrategy)) {
    throw new Error(`Unknown npm strategy: ${npmStrategy}`);
  }

  const options = {
    skipGradle: Boolean(parsed["skip-gradle"]),
    skipNpm: Boolean(parsed["skip-npm"]),
    skipCocoapods: Boolean(parsed["skip-cocoapods"]),
    allowPrerelease: Boolean(parsed["allow-prerelease"]),
    gradleKeys: splitCsv(parsed["gradle-keys"]),
    npmStrategy,
    projects: splitCsv(parsed.projects),
    cocoapodsPods: splitCsv(parsed["cocoapods-pods"]),
    cocoapodsRepoUpdate: Boolean(parsed["cocoapods-repo-update"]),
    output: parsed.output,
  };

  const root = repoRoot();
  if (mode === "report") {
    const outputPath = await renderReport(root, options);
    process.stdout.write(`${outputPath}\n`);
    return;
  }

  await performUpgrades(root, options);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
