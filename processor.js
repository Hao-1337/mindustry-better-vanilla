const { execSync } = require("child_process"), { copyFileSync, mkdirSync, readdirSync, statSync, unlinkSync } = require('fs'), { join } = require('path'), color = require('colors'), chokidar = require('chokidar');
color.enable();

const SOURCE_PATH = "D:\\Projects\\mindustry-better-vanilla\\build\\classes\\java\\main";
const TARGET_PATH = "C:\\Users\\hao1337\\AppData\\Roaming\\Mindustry\\mods\\hao-1337mindustry-better-vanilla";
const WATCH_PATH = "D:\\Projects\\mindustry-better-vanilla";
const ACCSET_PATH = "D:\\Projects\\mindustry-better-vanilla\\assets";
const ignored = [
  '**/node_modules/**',
  '**/gradle/**',
  '**/build/**',
  '**/.gradle/**',
  '**/.git/**',
  '**/bin/**',
  '**/.history/**',
  path => (path.endsWith('.java') || path.endsWith('.js') ) && path.split('\/').length === 4
];

const DIRS = readdirSync(SOURCE_PATH);

function isFile(target) {
  return statSync(target).isFile();
}

function copyFile(source, target) {
  try {
    copyFileSync(source, target);
  } catch (error) {
    if (error.code === 'EEXIST') {
      unlinkSync(target);
      copyFileSync(source, target);
    } else {
      throw error + error.stderr;
    }
  }
}

function copyFiles(source, target) {
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
        if (error.code !== 'EEXIST') {
          throw error;
        }
      }
      copyFiles(sourcePath, targetPath);
    }
  }
}

console.log("Start watching current project at".blue, WATCH_PATH.yellow);
var __ = false;

function javaChange(data) {
  if (__) return;
  __ = true;
  console.time("Complie completed in: ");
  console.log(`${'Detect java change at:'.blue} ${data.yellow.underline}${". Start complie".blue}`);

  try {
    execSync("npm run compile");
  } catch (e) {
    console.log('Compie error: '.red);
    console.error(e);
  }

  console.timeEnd("Complie completed in: ");
  __ = false;
  copyFiles(SOURCE_PATH, TARGET_PATH);
};

function accestChange(data) {
  console.log("Detect accest change at: ".green, data.yellow.underline);
  copyFiles(ACCSET_PATH, TARGET_PATH);
};

const java_watch = chokidar.watch(WATCH_PATH, {
  persistent: true,
  ignoreInitial: true,
  depth: 20,
  ignored: [...ignored, '**/assets/**']
});
const accest_watch = chokidar.watch(ACCSET_PATH, {
  persistent: true,
  ignoreInitial: true,
  depth: 20,
  ignored
});

java_watch.on('add', javaChange);
java_watch.on('change', javaChange);

accest_watch.on('add', accestChange);
accest_watch.on('change', accestChange);