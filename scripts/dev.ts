import { execSync } from "child_process";
import { copyFileSync, mkdirSync, readdirSync, statSync, unlinkSync, writeFileSync } from 'fs';
import { fileURLToPath } from "url";
import path, { join } from 'path';
import color from 'colors';
import chokidar from 'chokidar';
import dotenv from 'dotenv';
import { applyVersion } from "./platform.ts";
import { restore } from "./platform-revert.ts";
import AdmZip from "adm-zip";
import net from "net";

dotenv.config();
color.enable();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.join(__dirname, "..");
const assetsPath = path.join(__dirname, "..", "assets");
const sourcePath = path.join(__dirname, "..", "src");
const classPath = path.join(__dirname, "..", "build", "classes", "java", "main", "hao1337");
const givenEnv = (process.argv[2] || "steam") == "steam" ? "steam" : "jar";
const mindustryPath = path.resolve((givenEnv === "steam" ? process.env.MINDUSTRY_STEAM_PATH : process.env.MINDUSTRY_JAR_PATH) || ".");
const classpathModFolder = path.join(mindustryPath, "mods", "hao-1337mindustry-better-vanilla", "hao1337");
const modFolder = path.join(mindustryPath, "mods", "hao-1337mindustry-better-vanilla");
const assetsModFolder = path.join(mindustryPath, "mods", "hao-1337mindustry-better-vanilla");
const jarPath = Array.from(execSync("gradlew.bat printRuntimeClasspath", {
  cwd: projectRoot,
  stdio: "pipe"
}).toString().split("\n")).map(l => l.trim()).filter(l => l.endsWith(".jar"));

function extractJarClasses(jarPath: string, target: string) {
  const zip = new AdmZip(jarPath);

  for (const entry of zip.getEntries()) {
    if (!entry.entryName.endsWith(".class")) continue;

    const outPath = path.join(target, entry.entryName);

    mkdirSync(path.dirname(outPath), { recursive: true });
    writeFileSync(outPath, entry.getData());
  }
}

function extractAllDeps() {
  for (const jar of jarPath) {
    try {
      extractJarClasses(jar, modFolder);
    } catch (e) {
      console.error("Failed extracting:", jar, e);
    }
  }
}

function sendRestart() {
  const client = net.createConnection({ port: 1337, host: "127.0.0.1" }, () => {
    client.write("restart\n");
    client.end();
  });

  client.on("error", (err) => {
    console.log("DevTools seem to be not running:", err.message);
  });
}

const ignored = [
  '**/node_modules/**',
  '**/gradle/**',
  '**/build/**',
  '**/.gradle/**',
  '**/.git/**',
  '**/bin/**',
  '**/.history/**',
  (path: string) => (path.endsWith('.java') || path.endsWith('.js') ) && path.split('\/').length === 4
];

function isFile(target: string) {
  return statSync(target).isFile();
}

function copyFile(source: string, target: string) {
  try {
    copyFileSync(source, target);
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'EEXIST') {
      unlinkSync(target);
      copyFileSync(source, target);
    } else {
      throw error + (error as { stderr: string }).stderr;
    }
  }
}

function copyFiles(source: string, target: string) {
  const dirs = readdirSync(source);
  for (const d of dirs) {
    const sourcePath = join(source, d);
    const targetPath = join(target, d);
    if (isFile(sourcePath)) {
      copyFile(sourcePath, targetPath);
    } else {
      try {
        mkdirSync(targetPath);
      } catch (error) {
        if ((error as { code: string }).code !== 'EEXIST') {
          throw error;
        }
      }
      copyFiles(sourcePath, targetPath);
    }
  }
}

console.log("Start watching current project at".blue, projectRoot.yellow);
var __ = false;

function javaChange(data: string) {
  if (__) return;
  __ = true;
  console.time("Complie completed in: ");
  console.log(`${'Detect java change at:'.blue} ${data.yellow.underline}${". Start complie".blue}`);

  try {
    // applyVersion(givenEnv === "steam" ? "v147" : "v157");
    applyVersion("v157");
    execSync("npm run dev-compile");
    extractAllDeps();
    sendRestart();
  } catch (e) {
    console.log('Compie error: '.red);
    console.error((e as { stderr: string }).stderr.toString().red);
  }
  finally {
    restore();
  }

  console.timeEnd("Complie completed in: ");
  __ = false;
  // copyFiles(SOURCE_PATH, TARGET_PATH);
  copyFiles(classPath, classpathModFolder);
};

function accestChange(data: string) {
  console.log("Detect accest change at: ".green, data.yellow.underline);
  // copyFiles(ACCSET_PATH, TARGET_PATH);
  try {
    // applyVersion(givenEnv === "steam" ? "v147" : "v157");
    applyVersion("v157");
    copyFiles(assetsPath, assetsModFolder);
    sendRestart();
  } finally {
    restore();
  }
};

const java_watch = chokidar.watch(sourcePath, {
  persistent: true,
  ignoreInitial: true,
  depth: 20,
  ignored: [...ignored, '**/assets/**']
});
const accest_watch = chokidar.watch(assetsPath, {
  persistent: true,
  ignoreInitial: true,
  depth: 20,
  ignored
});

java_watch.on('add', javaChange);
java_watch.on('change', javaChange);

accest_watch.on('add', accestChange);
accest_watch.on('change', accestChange);