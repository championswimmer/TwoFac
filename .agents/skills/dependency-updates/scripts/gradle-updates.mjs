#!/usr/bin/env node

import path from "node:path";
import {
  cleanupTempFile,
  markdownTable,
  parseSimpleArgs,
  readText,
  repoRoot,
  runCommand,
  splitCsv,
  writeText,
  makeTempFile,
} from "./dependencyUpdateTools.mjs";

function isStable(version) {
  return !/(alpha|beta|rc|preview|snapshot|eap|dev|milestone|m\d)/i.test(version);
}

function parseCatalogRecords(text) {
  const lines = text.split(/\r?\n/);
  let inVersions = false;
  const records = [];

  for (let index = 0; index < lines.length; ) {
    const line = lines[index];
    const stripped = line.trim();

    if (stripped === "[versions]") {
      inVersions = true;
      index += 1;
      continue;
    }
    if (inVersions && stripped.startsWith("[") && stripped !== "[versions]") break;
    if (!inVersions) {
      index += 1;
      continue;
    }

    const match = line.match(/^([A-Za-z0-9._-]+)\s*=\s*"([^"]+)"\s*$/);
    if (!match) {
      index += 1;
      continue;
    }

    const [, key, current] = match;
    const candidates = [];
    let nextIndex = index + 1;
    while (nextIndex < lines.length) {
      const nextLine = lines[nextIndex].trim();
      if (!nextLine.startsWith("##")) break;
      const versions = [...lines[nextIndex].matchAll(/"([^"]+)"/g)].map((part) => part[1]);
      candidates.push(...versions);
      nextIndex += 1;
    }

    records.push({
      key,
      current,
      candidates,
      stableCandidates: candidates.filter(isStable),
    });
    index = nextIndex;
  }

  return records.map((record) => ({
    ...record,
    recommendedStable: record.stableCandidates.at(-1),
    latestSeen: record.candidates.at(-1),
  }));
}

function parseSettingsPlugins(settingsText) {
  return [...settingsText.matchAll(/id\("([^"]+)"\) version "([^"]+)"/g)].map((match) => [match[1], match[2]]);
}

function parseWrapperVersions(logText) {
  return [...logText.matchAll(/^\.\/gradlew wrapper --gradle-version (.+)$/gm)].map((match) => match[1]);
}

async function runRefreshVersions(root) {
  const catalogPath = path.join(root, "gradle", "libs.versions.toml");
  const versionsPropertiesPath = path.join(root, "versions.properties");
  const originalCatalog = await readText(catalogPath);
  const originalVersions = await readText(versionsPropertiesPath).catch(() => null);
  const tempLogPath = await makeTempFile("twofac-refreshversions", ".log");

  try {
    const result = await runCommand("./gradlew", ["refreshVersions", "--no-configuration-cache"], {
      cwd: root,
      timeoutMs: 300_000,
      maxBuffer: 100 * 1024 * 1024,
    });
    const logText = [result.stdout, result.stderr].filter(Boolean).join("\n");
    await writeText(tempLogPath, logText);
    const updatedCatalog = await readText(catalogPath);
    return {
      records: parseCatalogRecords(updatedCatalog),
      wrapperVersions: parseWrapperVersions(logText),
    };
  } finally {
    await writeText(catalogPath, originalCatalog);
    if (originalVersions !== null) {
      await writeText(versionsPropertiesPath, originalVersions);
    }
    await cleanupTempFile(tempLogPath);
  }
}

async function applyUpdates(root, records, options) {
  const { allowPrerelease = false, keys = [] } = options;
  const requestedKeys = new Set(keys);
  const catalogPath = path.join(root, "gradle", "libs.versions.toml");
  const lines = (await readText(catalogPath)).split(/\r?\n/);
  const updates = new Map();

  for (const record of records) {
    if (requestedKeys.size > 0 && !requestedKeys.has(record.key)) continue;
    const target = allowPrerelease ? record.latestSeen : record.recommendedStable;
    if (target && target !== record.current) {
      updates.set(record.key, target);
    }
  }

  if (updates.size === 0) return [];

  const changed = [];
  let inVersions = false;
  const newLines = [];

  for (const line of lines) {
    const stripped = line.trim();
    if (stripped === "[versions]") {
      inVersions = true;
      newLines.push(line);
      continue;
    }
    if (inVersions && stripped.startsWith("[") && stripped !== "[versions]") {
      inVersions = false;
    }

    if (inVersions) {
      const match = line.match(/^([A-Za-z0-9._-]+)\s*=\s*"([^"]+)"\s*$/);
      if (match) {
        const [, key, current] = match;
        if (updates.has(key)) {
          const target = updates.get(key);
          newLines.push(`${key} = "${target}"`);
          changed.push([key, current, target]);
          continue;
        }
      }
    }

    newLines.push(line);
  }

  await writeText(catalogPath, `${newLines.join("\n")}\n`);
  return changed;
}

async function renderReport(root, records, wrapperVersions) {
  const settingsPlugins = parseSettingsPlugins(await readText(path.join(root, "settings.gradle.kts")));
  const rows = [["Key", "Current", "Recommended stable", "Latest seen", "Notes"]];

  for (const record of records) {
    if (record.candidates.length === 0) continue;
    const notes = [];
    if (!record.recommendedStable) {
      notes.push("pre-release only");
    } else if (record.latestSeen && record.latestSeen !== record.recommendedStable) {
      notes.push("newer pre-release available");
    }
    rows.push([
      record.key,
      record.current,
      record.recommendedStable ?? "—",
      record.latestSeen ?? "—",
      notes.join(", "),
    ]);
  }

  const pluginRows = [["Plugin", "Pinned version"], ...settingsPlugins];
  const wrapperNote = wrapperVersions.length > 0
    ? wrapperVersions.map((version) => `- \`./gradlew wrapper --gradle-version ${version}\``).join("\n")
    : "- No wrapper suggestions were emitted by `refreshVersions`.";

  return `## Gradle / Kotlin dependency surface

- Modules sharing the central version catalog: \`sharedLib\`, \`composeApp\`, \`cliApp\`, \`androidApp\`, \`watchApp\`
- Primary audit source: \`gradle/libs.versions.toml\`
- Audit command: \`./gradlew refreshVersions\`
- This repo currently keeps dependency versions in the version catalog; \`versions.properties\` is effectively only refreshVersions metadata.

### Stable catalog update candidates

${rows.length > 1 ? markdownTable(rows) : "No catalog updates were reported."}

### Hardcoded settings plugins to review manually

${markdownTable(pluginRows)}

### Gradle wrapper suggestions from refreshVersions

${wrapperNote}
`;
}

function renderUpgradeSummary(changed, allowPrerelease) {
  const strategy = allowPrerelease
    ? "latest available version including pre-releases"
    : "latest stable version";
  if (changed.length === 0) {
    return `## Gradle upgrade result\n\nNo catalog entries changed for strategy: ${strategy}.\n`;
  }

  const lines = [
    "## Gradle upgrade result",
    "",
    `Applied ${strategy} to \`gradle/libs.versions.toml\`:`,
    "",
    ...changed.map(([key, current, target]) => `- \`${key}\`: \`${current}\` -> \`${target}\``),
  ];
  return `${lines.join("\n")}\n`;
}

function printHelp() {
  console.log(`Gradle dependency audit/upgrade helper

Usage:
  node .agents/skills/dependency-updates/scripts/gradle-updates.mjs [mode] [options]

Modes:
  report   Audit via refreshVersions (default)
  upgrade  Apply selected updates to gradle/libs.versions.toml

Options:
  --allow-prerelease   Allow upgrade mode to use pre-release versions
  --keys <csv>         Limit upgrade mode to specific version keys
  --help               Show this help
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
  const { records, wrapperVersions } = await runRefreshVersions(root);

  if (mode === "report") {
    process.stdout.write(await renderReport(root, records, wrapperVersions));
    return;
  }

  const changed = await applyUpdates(root, records, {
    allowPrerelease: Boolean(parsed["allow-prerelease"]),
    keys: splitCsv(parsed.keys),
  });
  process.stdout.write(renderUpgradeSummary(changed, Boolean(parsed["allow-prerelease"])));
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
