import { spawn } from "child_process";
import fs from "fs";
import { fileURLToPath, pathToFileURL } from "url";
import path from "path";
import dotenv from "dotenv";
import * as tar from "tar";
import { LSPClient } from "./java-lsp.ts";

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const JDTLS = "./jdt-language-server";
const projectRoot = pathToFileURL(path.join(__dirname, "..")).href;
const WORKSPACE = process.env.JDTLS_WORKSPACE || path.join(__dirname, "workspace");

async function startJavaLanguageServer() {
    if (!fs.existsSync(JDTLS)) {
        if (!fs.existsSync("./jdt-language-server-latest.tar.gz")) {
            const downloadUrl = "https://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz";
            const response = await fetch(downloadUrl);
            if (!response.ok) throw new Error(`Failed to download JDTLS: ${response.statusText}`);

            const buffer = await response.arrayBuffer();
            fs.writeFileSync("./jdt-language-server-latest.tar.gz", Buffer.from(buffer));
        }

        fs.existsSync(JDTLS) || fs.mkdirSync(JDTLS);
        await tar.x({
            file: "./jdt-language-server-latest.tar.gz",
            cwd: JDTLS,
        });
    }

    const config = process.platform === "win32" ? "config_win" : process.platform === "darwin" ? "config_mac" : "config_linux";
    const launcher = fs.readdirSync(path.join(JDTLS, "plugins")).find((f) => /^org\.eclipse\.equinox\.launcher_\d+.*\.jar$/.test(f));
    if (!launcher) throw new Error("Could not find launcher jar in plugins directory");

    const server = spawn("java", [
        "-Declipse.application=org.eclipse.jdt.ls.core.id1",
        "-Dosgi.bundles.defaultStartLevel=4",
        "-Declipse.product=org.eclipse.jdt.ls.core.product",
        "-Xmx1G",
        "-jar",
        path.join(JDTLS, "plugins", launcher),
        "-configuration",
        path.join(JDTLS, config),
        "-data",
        WORKSPACE,
    ]);

    return server;
}

async function main() {
    const server = await startJavaLanguageServer();
    const client = new LSPClient(server);

    await client.request("initialize", {
        processId: process.pid,
        rootUri: projectRoot,
        capabilities: {},
    });

    await client.request("initialized", {});
}

await main();