#!/usr/bin/env node

import {
  listSimulators,
  pickSimulator,
  bootSimulator,
  toShellExports,
} from "./simulatorTools.mjs";

function printHelp() {
  console.log(`iOS simulator picker

Usage:
  node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs [options]

Options:
  --list            List available simulators and exit
  --json            Print result JSON
  --shell           Print shell exports (for eval)
  --boot            Boot selected simulator and wait for readiness
  --udid <id>       Select simulator by UDID (non-interactive)
  --help            Show this help
`);
}

function parseArgs(argv) {
  const args = {
    list: false,
    json: false,
    shell: false,
    boot: false,
    udid: undefined,
  };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--list") args.list = true;
    else if (arg === "--json") args.json = true;
    else if (arg === "--shell") args.shell = true;
    else if (arg === "--boot") args.boot = true;
    else if (arg === "--udid") {
      if (i + 1 >= argv.length) throw new Error("--udid requires a value");
      args.udid = argv[i + 1];
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

  const simulators = await listSimulators();
  if (args.list) {
    if (args.json) {
      console.log(JSON.stringify(simulators, null, 2));
      return;
    }
    if (simulators.length === 0) {
      console.log("No iOS simulators found.");
      return;
    }
    console.log("Available iOS simulators:");
    for (const sim of simulators) {
      const status = sim.state === "Booted" ? "booted" : "shutdown";
      console.log(`- ${sim.name} (${sim.runtimeName}) [${status}] • ${sim.udid}`);
    }
    return;
  }

  let selected;
  if (args.udid) {
    selected = simulators.find((entry) => entry.udid === args.udid);
    if (!selected) {
      throw new Error(`Simulator UDID not found: ${args.udid}`);
    }
  } else {
    selected = await pickSimulator(simulators);
  }

  if (args.boot) {
    await bootSimulator(selected.udid);
  }

  const result = {
    udid: selected.udid,
    name: selected.name,
    runtimeName: selected.runtimeName,
    state: args.boot ? "Booted" : selected.state,
    destination: `platform=iOS Simulator,id=${selected.udid}`,
  };

  if (args.shell) {
    console.log(
      toShellExports({
        IOS_SIMULATOR_UDID: result.udid,
        IOS_SIMULATOR_DESTINATION: result.destination,
      })
    );
    return;
  }

  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
    return;
  }

  console.log(`Selected simulator: ${result.name} (${result.runtimeName})`);
  console.log(`UDID: ${result.udid}`);
  console.log(`Destination: ${result.destination}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});

