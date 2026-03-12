import fs from "fs";
import path from "path";
import { glob } from "glob";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const projectRoot = path.join(__dirname, "..");
const CLONE_DIR = path.resolve(__dirname, ".cloned");

export function restore() {
    const files = glob.sync("**/*", {
        cwd: CLONE_DIR,
        absolute: true,
        nodir: true
    });

    for (const file of files) {
        const rel = path.relative(CLONE_DIR, file);
        const target = path.resolve(projectRoot, rel);

        fs.mkdirSync(path.dirname(target), { recursive: true });
        fs.copyFileSync(file, target);

        // console.log("Restored:", target);
    }

    if (fs.existsSync(CLONE_DIR)) fs.rmSync(CLONE_DIR, { recursive: true });
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
    restore();
}