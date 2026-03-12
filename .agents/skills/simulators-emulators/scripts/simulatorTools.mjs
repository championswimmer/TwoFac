#!/usr/bin/env node

import { execFile, spawn } from "node:child_process";
import { promisify } from "node:util";
import readline from "node:readline/promises";
import { stdin, stdout } from "node:process";

const execFileAsync = promisify(execFile);

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, `'\\''`)}'`;
}

export function toShellExports(values) {
  return Object.entries(values)
    .filter(([, value]) => value !== undefined && value !== null)
    .map(([key, value]) => `export ${key}=${shellQuote(value)}`)
    .join("\n");
}

async function runCommand(command, args = [], options = {}) {
  const { allowFailure = false, timeoutMs = 30_000 } = options;
  try {
    const { stdout: out, stderr } = await execFileAsync(command, args, {
      timeout: timeoutMs,
      maxBuffer: 10 * 1024 * 1024,
    });
    return {
      code: 0,
      stdout: out?.toString() ?? "",
      stderr: stderr?.toString() ?? "",
    };
  } catch (error) {
    if (!allowFailure) {
      const message = [
        `Command failed: ${command} ${args.join(" ")}`,
        error.stdout ? `stdout:\n${error.stdout.toString()}` : "",
        error.stderr ? `stderr:\n${error.stderr.toString()}` : "",
      ]
        .filter(Boolean)
        .join("\n");
      throw new Error(message);
    }

    return {
      code: typeof error.code === "number" ? error.code : 1,
      stdout: error.stdout?.toString() ?? "",
      stderr: error.stderr?.toString() ?? "",
    };
  }
}

function parseAvdManagerDetailedOutput(raw) {
  const entries = [];
  const lines = raw.split(/\r?\n/);
  let current = null;

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) {
      continue;
    }

    if (trimmed.startsWith("Name:")) {
      if (current?.avdName) entries.push(current);
      current = { avdName: trimmed.replace(/^Name:\s*/, "") };
      continue;
    }

    if (!current) {
      continue;
    }

    if (trimmed.startsWith("Device:")) {
      current.device = trimmed.replace(/^Device:\s*/, "");
      continue;
    }
    if (trimmed.startsWith("Path:")) {
      current.path = trimmed.replace(/^Path:\s*/, "");
      continue;
    }
    if (trimmed.startsWith("Target:")) {
      current.target = trimmed.replace(/^Target:\s*/, "");
      continue;
    }
    if (trimmed.includes("Tag/ABI:")) {
      const match = trimmed.match(/Tag\/ABI:\s*(.+)$/);
      if (match) current.tagAbi = match[1];
      continue;
    }
  }

  if (current?.avdName) entries.push(current);
  return entries;
}

async function listAvdNames() {
  const namesFromAvdManager = await runCommand("avdmanager", ["list", "avd", "-c"], {
    allowFailure: true,
  });
  const cleanAvdManagerNames = namesFromAvdManager.stdout
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.includes(":") && !line.startsWith("Available"));

  if (cleanAvdManagerNames.length > 0) {
    return cleanAvdManagerNames;
  }

  const fromEmulator = await runCommand("emulator", ["-list-avds"]);
  return fromEmulator.stdout
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

async function listAvdMetadata() {
  const detailed = await runCommand("avdmanager", ["list", "avd"], { allowFailure: true });
  if (!detailed.stdout.trim()) return [];
  return parseAvdManagerDetailedOutput(detailed.stdout);
}

async function listRunningAndroidEmulators() {
  const adbList = await runCommand("adb", ["devices", "-l"]);
  const lines = adbList.stdout.split(/\r?\n/).slice(1).map((line) => line.trim()).filter(Boolean);
  const emulatorLines = lines.filter((line) => line.startsWith("emulator-"));
  const running = [];

  for (const line of emulatorLines) {
    const parts = line.split(/\s+/);
    const serial = parts[0];
    const state = parts[1] ?? "unknown";

    const avdNameResult = await runCommand("adb", ["-s", serial, "emu", "avd", "name"], {
      allowFailure: true,
    });
    const avdName = avdNameResult.stdout.split(/\r?\n/).map((x) => x.trim()).find(Boolean);

    running.push({
      serial,
      state,
      avdName: avdName || undefined,
    });
  }
  return running;
}

export async function listEmulators() {
  const [avdNames, metadata, running] = await Promise.all([
    listAvdNames(),
    listAvdMetadata(),
    listRunningAndroidEmulators(),
  ]);
  const metadataByName = new Map(metadata.map((entry) => [entry.avdName, entry]));
  const runningByAvd = new Map(running.filter((r) => r.avdName).map((r) => [r.avdName, r]));

  const result = avdNames.map((avdName) => {
    const details = metadataByName.get(avdName) || {};
    const runningEntry = runningByAvd.get(avdName);
    return {
      id: avdName,
      avdName,
      device: details.device,
      target: details.target,
      tagAbi: details.tagAbi,
      path: details.path,
      running: Boolean(runningEntry),
      serial: runningEntry?.serial,
      state: runningEntry?.state ?? "stopped",
    };
  });

  const orphans = running
    .filter((entry) => entry.avdName && !avdNames.includes(entry.avdName))
    .map((entry) => ({
      id: entry.serial,
      avdName: entry.avdName,
      running: true,
      serial: entry.serial,
      state: entry.state,
      orphaned: true,
    }));

  return [...result, ...orphans];
}

export async function pickEmulator(emulators) {
  if (!Array.isArray(emulators) || emulators.length === 0) {
    throw new Error("No Android emulators were found.");
  }
  if (!stdin.isTTY || !stdout.isTTY) {
    throw new Error("Interactive picker requires a TTY. Use --avd <name> for non-interactive use.");
  }

  console.log("\nSelect an Android emulator:\n");
  emulators.forEach((entry, index) => {
    const status = entry.running ? "running" : "stopped";
    const details = [entry.device, entry.tagAbi].filter(Boolean).join(" • ");
    const serial = entry.serial ? ` • ${entry.serial}` : "";
    console.log(`${index + 1}. ${entry.avdName} [${status}]${serial}${details ? ` • ${details}` : ""}`);
  });
  console.log("");

  const rl = readline.createInterface({ input: stdin, output: stdout });
  try {
    const answer = await rl.question("Enter selection number: ");
    const idx = Number.parseInt(answer.trim(), 10);
    if (!Number.isInteger(idx) || idx < 1 || idx > emulators.length) {
      throw new Error(`Invalid selection: ${answer.trim()}`);
    }
    return emulators[idx - 1];
  } finally {
    rl.close();
  }
}

function nextAvailableEmulatorConsolePort(runningSerials) {
  const used = new Set(
    runningSerials
      .map((serial) => Number.parseInt(serial.replace(/^emulator-/, ""), 10))
      .filter((n) => Number.isInteger(n))
      .map((adbPort) => adbPort - 1)
  );
  for (let consolePort = 5554; consolePort <= 5680; consolePort += 2) {
    if (!used.has(consolePort)) return consolePort;
  }
  throw new Error("No free emulator console ports available in range 5554-5680.");
}

export async function waitForAndroidBoot(serial, options = {}) {
  const timeoutMs = options.timeoutMs ?? 300_000;
  const pollMs = options.pollMs ?? 2_000;
  const deadline = Date.now() + timeoutMs;

  await runCommand("adb", ["-s", serial, "wait-for-device"], {
    timeoutMs: timeoutMs,
  });

  while (Date.now() < deadline) {
    const sysBoot = await runCommand("adb", ["-s", serial, "shell", "getprop", "sys.boot_completed"], {
      allowFailure: true,
      timeoutMs: 10_000,
    });
    const devBoot = await runCommand("adb", ["-s", serial, "shell", "getprop", "dev.bootcomplete"], {
      allowFailure: true,
      timeoutMs: 10_000,
    });
    if (sysBoot.stdout.trim() === "1" || devBoot.stdout.trim() === "1") {
      return;
    }
    await sleep(pollMs);
  }

  throw new Error(`Timed out waiting for Android emulator ${serial} to finish booting.`);
}

export async function startEmulatorAndWait(avdName, options = {}) {
  const emulators = await listEmulators();
  const match = emulators.find((entry) => entry.avdName === avdName);
  if (!match) {
    throw new Error(`Unknown AVD: ${avdName}`);
  }
  if (match.running && match.serial) {
    await waitForAndroidBoot(match.serial, options);
    return {
      avdName,
      serial: match.serial,
      alreadyRunning: true,
    };
  }

  await runCommand("adb", ["start-server"]);
  const runningSerials = emulators.filter((entry) => entry.running && entry.serial).map((entry) => entry.serial);
  const consolePort = nextAvailableEmulatorConsolePort(runningSerials);
  const adbSerial = `emulator-${consolePort + 1}`;

  const args = [`@${avdName}`, "-port", String(consolePort), "-no-boot-anim", "-no-snapshot-load"];
  if (options.headless) args.push("-no-window");

  const child = spawn("emulator", args, {
    detached: true,
    stdio: "ignore",
  });
  child.unref();

  await waitForAndroidBoot(adbSerial, options);
  return {
    avdName,
    serial: adbSerial,
    alreadyRunning: false,
  };
}

export async function listSimulators(options = {}) {
  const platform = options.platform ?? "iOS";
  const includeUnavailable = options.includeUnavailable ?? false;

  const list = await runCommand("xcrun", ["simctl", "list", "--json"]);
  let parsed;
  try {
    parsed = JSON.parse(list.stdout);
  } catch (error) {
    throw new Error(`Could not parse simctl JSON output: ${error.message}`);
  }

  const runtimeById = new Map((parsed.runtimes || []).map((runtime) => [runtime.identifier, runtime]));
  const results = [];

  for (const [runtimeId, devices] of Object.entries(parsed.devices || {})) {
    const runtime = runtimeById.get(runtimeId);
    if (!runtime) continue;
    if (platform && runtime.platform !== platform) continue;

    for (const device of devices) {
      const available = Boolean(device.isAvailable) && Boolean(runtime.isAvailable);
      if (!includeUnavailable && !available) continue;
      results.push({
        id: device.udid,
        udid: device.udid,
        name: device.name,
        state: device.state,
        isAvailable: available,
        runtimeId: runtime.identifier,
        runtimeName: runtime.name,
        runtimeVersion: runtime.version,
      });
    }
  }

  results.sort((a, b) => {
    if (a.state === "Booted" && b.state !== "Booted") return -1;
    if (a.state !== "Booted" && b.state === "Booted") return 1;
    if (a.runtimeVersion !== b.runtimeVersion) return String(b.runtimeVersion).localeCompare(String(a.runtimeVersion));
    return a.name.localeCompare(b.name);
  });

  return results;
}

export async function pickSimulator(simulators) {
  if (!Array.isArray(simulators) || simulators.length === 0) {
    throw new Error("No iOS simulators were found.");
  }
  if (!stdin.isTTY || !stdout.isTTY) {
    throw new Error("Interactive picker requires a TTY. Use --udid <id> for non-interactive use.");
  }

  console.log("\nSelect an iOS simulator:\n");
  simulators.forEach((entry, index) => {
    const status = entry.state === "Booted" ? "booted" : "shutdown";
    console.log(`${index + 1}. ${entry.name} (${entry.runtimeName}) [${status}] • ${entry.udid}`);
  });
  console.log("");

  const rl = readline.createInterface({ input: stdin, output: stdout });
  try {
    const answer = await rl.question("Enter selection number: ");
    const idx = Number.parseInt(answer.trim(), 10);
    if (!Number.isInteger(idx) || idx < 1 || idx > simulators.length) {
      throw new Error(`Invalid selection: ${answer.trim()}`);
    }
    return simulators[idx - 1];
  } finally {
    rl.close();
  }
}

export async function bootSimulator(udid) {
  await runCommand("xcrun", ["simctl", "bootstatus", udid, "-b"], {
    timeoutMs: 300_000,
  });
  return { udid };
}

