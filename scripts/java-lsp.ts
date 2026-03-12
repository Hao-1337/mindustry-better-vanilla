import type { ChildProcessWithoutNullStreams } from "child_process";

export class LSPClient {
    private buffer = "";
    private id = 1;
    private pending = new Map<number, (v: any) => void>();
    private server: ChildProcessWithoutNullStreams;

    constructor(server: ChildProcessWithoutNullStreams) {
        this.server = server;
        this.server.stdout.on("data", (chunk) => this.handle(chunk.toString()));

        this.server.stdout.on("data", (data) => {
            console.log(`Server: ${data}`);
        });

        this.server.stderr.on("data", (data) => {
            if (data.toString().includes(`Cannot invoke "java.util.concurrent.CompletableFuture.thenAccept(java.util.function.Consumer)" because "future" is null`)) return;
            console.error("JDTLS ERR:", data.toString());
        });

        this.server.on("exit", (code, signal) => {
            console.log("JDTLS EXIT:", code, signal);
        });

        this.server.on("error", (err) => {
            console.error("SPAWN ERROR:", err);
        });
    }

    private handle(chunk: string) {
        this.buffer += chunk;

        while (true) {
            const headerEnd = this.buffer.indexOf("\r\n\r\n");
            if (headerEnd === -1) return;

            const header = this.buffer.slice(0, headerEnd);
            const match = header.match(/Content-Length: (\d+)/i);
            if (!match) return;

            const length = Number(match[1]);
            const start = headerEnd + 4;

            if (this.buffer.length < start + length) return;

            const body = this.buffer.slice(start, start + length);
            this.buffer = this.buffer.slice(start + length);

            const msg = JSON.parse(body);

            if (msg.id && this.pending.has(msg.id)) {
                this.pending.get(msg.id)!(msg.result);
                this.pending.delete(msg.id);
            }
        }
    }

    request(method: string, params: any) {
        const id = this.id++;

        const msg = {
            jsonrpc: "2.0",
            id,
            method,
            params,
        };

        const json = JSON.stringify(msg);
        const payload = `Content-Length: ${Buffer.byteLength(json)}\r\n\r\n${json}`;

        this.server.stdin.write(payload);

        return new Promise((resolve) => {
            this.pending.set(id, resolve);
        });
    }

    notify(method: string, params: any) {
        const msg = { jsonrpc: "2.0", method, params };
        const json = JSON.stringify(msg);
        const payload = `Content-Length: ${Buffer.byteLength(json)}\r\n\r\n${json}`;
        this.server.stdin.write(payload);
    }
}
