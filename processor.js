const { execSync } = require("child_process");
const { copyFileSync, mkdirSync, readdirSync, statSync, unlinkSync } = require('fs');
const { join } = require('path');

const SOURCE_PATH = "D:\\Project\\Hao1337 Mindustry\\build\\classes\\java\\main";
const TARGET_PATH = "C:\\Users\\Admin\\AppData\\Roaming\\Mindustry\\mods\\hao1337mindustry-better-vanilla";
const ACCSET_PATH = "D:\\Project\\Hao1337 Mindustry\\assets";
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
  

execSync("npm run compile");
copyFiles(SOURCE_PATH, TARGET_PATH);
copyFiles(ACCSET_PATH, TARGET_PATH);