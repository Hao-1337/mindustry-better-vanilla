package hao1337.addins;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import hao1337.Version;
import mindustry.Vars;

public class DevTools {
    static File jarFile = null;
    static Fi modFile = null;
    static boolean isDev = false;
    static int port = 1337;
    static volatile boolean restarting = false;
    static long lastTrigger = 0;

    static public void init() {
        check();
        if (isDev) {
            Log.warn("[[" + Version.name
                    + "] Development tools is running, if you are not doing any development, please turn this off!");
            start();
        }
    }

    static void check() {
        try {
            if (Vars.headless || Vars.mobile) {
                Log.err("Dev tools only support for desktop!");
                isDev = false;
                return;
            }

            jarFile = new File(Vars.player.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.getName().endsWith(".jar")) {
                Log.err("Not a valid executable somehow?");
                isDev = false;
                return;
            }

            modFile = Vars.modDirectory.child(Version.unzipName);
            if (!modFile.exists()) {
                Log.err("Not a valid executable somehow?");
                isDev = false;
                return;
            }

            isDev = Core.settings.getBool("hao1337.mod.devtools.server");
            port = Core.settings.getInt("hao1337.mod.devtools.port");
        } catch (URISyntaxException e) {
        }
    }

    static void start() {
        if (!isDev) {
            Core.settings.put("hao1337.mod.devtools.server", false);
            return;
        }

        new Thread(() -> {
            try (var server = new java.net.ServerSocket(port, 0, java.net.InetAddress.getByName("127.0.0.1"))) {
                while (true) {
                    try (var socket = server.accept()) {
                        socket.setSoTimeout(1000);
                        var reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));

                        String line = reader.readLine();
                        if (!"restart".equals(line))
                            continue;

                        long now = System.currentTimeMillis();

                        if (now - lastTrigger < 2000)
                            continue;
                        lastTrigger = now;

                        if (restarting)
                            continue;
                        restarting = true;

                        new Thread(() -> {
                            try {
                                restart();
                            } finally {
                                restarting = false;
                            }
                        }).start();
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                Log.err("Dev server failed", e);
            }
        }, "dev-tools-server").start();

        Log.info("[[" + Version.name + "] DevTools HTTP server running on 127.0.0.1:" + port);
    }

    static void restart() {
        try {
            var info = ProcessHandle.current().info();
            String java = info.command().orElseThrow();
            String[] args = info.arguments().orElse(new String[0]);

            List<String> cmd = new ArrayList<>();
            cmd.add(java);
            Collections.addAll(cmd, args);

            new ProcessBuilder(cmd)
                .inheritIO()
                .start();

            System.exit(0);
        } catch (Exception e) {
            Log.err("Restart failed", e);
        }
    }
}
