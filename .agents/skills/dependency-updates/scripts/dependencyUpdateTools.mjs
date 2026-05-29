#!/usr/bin/env node

import { execFile } from "node:child_process";
import { existsSync as fsExistsSync, promises as fs } from "node:fs";
import path from "node:path";
import os from "node:os";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

export const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url));

export function repoRoot() {
  let current = SCRIPT_DIR;
  while (true) {
    if (fsExistsSync(path.join(current, "settings.gradle.kts")) && fsExistsSync(path.join(current, ".agents"))) {
      return current;
    }
    const parent = path.dirname(current);
    if (parent === current) {
      throw new Error("Could not locate repository root");
    }
    current = parent;
  }
}

export async function exists(target) {
  try {
    await fs.access(target);
    return true;
  } catch {
    return false;
  }
}

export async function readText(target) {
  return fs.readFile(target, "utf8");
}

export async function writeText(target, content) {
  await fs.writeFile(target, content, "utf8");
}

export async function ensureDir(target) {
  await fs.mkdir(target, { recursive: true });
}

export async function makeTempFile(prefix, suffix = ".tmp") {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), `${prefix}-`));
  return path.join(directory, `file${suffix}`);
}

export async function cleanupTempFile(filePath) {
  await fs.rm(path.dirname(filePath), { recursive: true, force: true });
}

export async function runCommand(command, args = [], options = {}) {
  const {
    cwd,
    allowFailure = false,
    timeoutMs = 300_000,
    maxBuffer = 50 * 1024 * 1024,
  } = options;

  try {
    const { stdout, stderr } = await execFileAsync(command, args, {
      cwd,
      timeout: timeoutMs,
      maxBuffer,
    });
    return {
      code: 0,
      stdout: stdout?.toString() ?? "",
      stderr: stderr?.toString() ?? "",
    };
  } catch (error) {
    if (!allowFailure) {
      const message = [
        `Command failed: ${command} ${args.join(" ")}`,
        error.stdout ? `stdout:\n${error.stdout.toString()}` : "",
        error.stderr ? `stderr:\n${error.stderr.toString()}` : "",
        error.message || "",
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

export function markdownTable(rows) {
  if (!Array.isArray(rows) || rows.length === 0) return "";
  const header = `| ${rows[0].join(" | ")} |`;
  const separator = `| ${rows[0].map(() => "---").join(" | ")} |`;
  const body = rows.slice(1).map((row) => `| ${row.join(" | ")} |`);
  return [header, separator, ...body].join("\n");
}

export function parseSimpleArgs(argv) {
  const args = {
    _: [],
  };

  for (let index = 0; index < argv.length; index += 1) {
    const part = argv[index];
    if (!part.startsWith("--")) {
      args._.push(part);
      continue;
    }

    const key = part.slice(2);
    const next = argv[index + 1];
    if (!next || next.startsWith("--")) {
      args[key] = true;
      continue;
    }

    args[key] = next;
    index += 1;
  }

  return args;
}

export function splitCsv(value) {
  if (!value) return [];
  return String(value)
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean);
}

export function formatTimestamp(date = new Date()) {
  const pad = (value) => String(value).padStart(2, "0");
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join("") + `-${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`;
}

export function isoSeconds(date = new Date()) {
  return new Date(date.getTime() - date.getMilliseconds()).toISOString().replace(/\.000Z$/, "Z");
}

export function shellPath(root, relativePath) {
  return path.join(root, relativePath);
}
