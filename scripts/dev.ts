import { execSync } from "child_process";
import { copyFileSync, mkdirSync, readdirSync, statSync, unlinkSync } from 'fs';
import { fileURLToPath } from "url";
import path, { join } from 'path';
import color from 'colors';
import chokidar from 'chokidar';
import dotenv from 'dotenv';
import { applyVersion } from "./platform.ts";
import { restore } from "./platform-revert.ts";

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
const assetsModFolder = path.join(mindustryPath, "mods", "hao-1337mindustry-better-vanilla");

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
    execSync("npm run compile");
  } catch (e) {
    console.log('Compie error: '.red);
    console.error((e as { stderr: string }).stderr.toString().red);
  }

  console.timeEnd("Complie completed in: ");
  __ = false;
  // copyFiles(SOURCE_PATH, TARGET_PATH);
  copyFiles(classPath, classpathModFolder);
};

function accestChange(data: string) {
  console.log("Detect accest change at: ".green, data.yellow.underline);
  // copyFiles(ACCSET_PATH, TARGET_PATH);
  applyVersion(givenEnv === "steam" ? "v147" : "v154");
  copyFiles(assetsPath, assetsModFolder);
  restore();
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