#!/usr/bin/env node

import {
  listEmulators,
  pickEmulator,
  startEmulatorAndWait,
  toShellExports,
} from "./simulatorTools.mjs";

function printHelp() {
  console.log(`Android emulator picker

Usage:
  node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs [options]

Options:
  --list            List known emulators and exit
  --json            Print result JSON
  --shell           Print shell exports (for eval)
  --boot            Boot selected emulator and wait for readiness
  --headless        Boot with -no-window (use with --boot)
  --avd <name>      Select emulator by AVD name (non-interactive)
  --help            Show this help
`);
}

function parseArgs(argv) {
  const args = {
    list: false,
    json: false,
    shell: false,
    boot: false,
    headless: false,
    avd: undefined,
  };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--list") args.list = true;
    else if (arg === "--json") args.json = true;
    else if (arg === "--shell") args.shell = true;
    else if (arg === "--boot") args.boot = true;
    else if (arg === "--headless") args.headless = true;
    else if (arg === "--avd") {
      if (i + 1 >= argv.length) throw new Error("--avd requires a value");
      args.avd = argv[i + 1];
      i += 1;
    } else if (arg === "--help" || arg === "-h") {
      args.help = true;
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  return args;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    printHelp();
    return;
  }

  const emulators = await listEmulators();
  if (args.list) {
    if (args.json) {
      console.log(JSON.stringify(emulators, null, 2));
      return;
    }
    if (emulators.length === 0) {
      console.log("No Android emulators found.");
      return;
    }
    console.log("Available Android emulators:");
    for (const emulator of emulators) {
      const status = emulator.running ? "running" : "stopped";
      const details = [emulator.device, emulator.tagAbi].filter(Boolean).join(" • ");
      const serial = emulator.serial ? ` • ${emulator.serial}` : "";
      console.log(`- ${emulator.avdName} [${status}]${serial}${details ? ` • ${details}` : ""}`);
    }
    return;
  }

  let selected;
  if (args.avd) {
    selected = emulators.find((entry) => entry.avdName === args.avd);
    if (!selected) {
      throw new Error(`AVD not found: ${args.avd}`);
    }
  } else {
    selected = await pickEmulator(emulators);
  }

  let result = {
    avdName: selected.avdName,
    serial: selected.serial,
    running: selected.running,
  };

  if (args.boot) {
    const booted = await startEmulatorAndWait(selected.avdName, { headless: args.headless });
    result = {
      avdName: booted.avdName,
      serial: booted.serial,
      running: true,
      alreadyRunning: booted.alreadyRunning,
    };
  }

  if (args.shell) {
    console.log(
      toShellExports({
        ANDROID_AVD_NAME: result.avdName,
        ANDROID_SERIAL: result.serial ?? "",
      })
    );
    return;
  }

  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
    return;
  }

  console.log(`Selected AVD: ${result.avdName}`);
  if (result.serial) {
    console.log(`Android serial: ${result.serial}`);
  } else {
    console.log("No running serial yet. Re-run with --boot to launch and wait.");
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});

