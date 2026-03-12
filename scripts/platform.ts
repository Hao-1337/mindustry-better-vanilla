import fs from "fs";
import path from "path";
import { glob } from "glob";
import { fileURLToPath } from "url";
import json5 from "json5";

interface Replacement {
    class?: string;
    regex?: boolean;
    oldField: string;
    newField: string;
}

interface VersionConfig {
    name: string;
    version: string;
    replacements: Record<string, Replacement[]>;
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.join(__dirname, "..");

const CONFIG = json5.parse(fs.readFileSync("./platforms.json", "utf8")) as VersionConfig[];
const CLONE_DIR = path.resolve(__dirname, ".cloned");

function ensureDir(filePath: string) {
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function backup(file: string) {
    const rel = path.relative(projectRoot, file);
    const target = path.join(CLONE_DIR, rel);

    console.log("Backing up:", file, "to", target);

    if (!fs.existsSync(target)) {
        ensureDir(target);
        fs.copyFileSync(file, target);
    }
}

function applyReplacement(file: string, replacements: Replacement[]) {
    let content = fs.readFileSync(file, "utf8");

    for (const r of replacements) {
        if (r.regex) {
            const match = r.oldField.match(/^\/(.+)\/([gimsuy]*)$/);
            if (!match) continue;

            const pattern = new RegExp(match[1]!, match[2]);
            content = content.replace(pattern, r.newField);
        } else {
            const oldField = r.oldField;
            const newField = r.newField;
            content = content.split(oldField).join(newField);
        }
    }

    fs.writeFileSync(file, content);
}

export function applyVersion(name: string) {
    const version = CONFIG.find((v) => v.version === name);

    if (!version) throw new Error(`Version not found: ${name}`);
    console.log("Applying version:", version.name);

    for (const pattern in version.replacements) {
        const files = glob.sync(pattern, { cwd: __dirname, absolute: true });

        for (const file of files) {
            backup(file);
            applyReplacement(file, version.replacements[pattern]!);
            console.log("Patched:", file);
        }
    }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
    const target = process.argv[2];
    if (!target) {
        console.error("Usage: node applyVersion.ts <versionName>");
        process.exit(1);
    }

    applyVersion(target);
}
