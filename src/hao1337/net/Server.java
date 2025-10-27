package hao1337.net;

import static mindustry.Vars.*;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Cons2;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectIntMap.Entry;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Timer;
import arc.util.Timer.Task;
import hao1337.Main;
import mindustry.game.EventType;
import mindustry.game.EventType.ClientServerConnectEvent;
import mindustry.game.EventType.HostEvent;
import mindustry.net.NetConnection;
import mindustry.net.Packet;

public class Server {
    public static final float WAIT_TIME = 8f;
    public static final boolean ENABLE = true;
    private static final int AUTH_TIMEOUT_TICKS = (int) (WAIT_TIME * 4);

    private static final ObjectIntMap<NetConnection> playerConnections = new ObjectIntMap<>();
    private static final Seq<NetConnection> players = new Seq<>();
    private static final Seq<HaoNetPackage> clientQueue = new Seq<>();
    private static final Seq<HaoNetPackage> sender = new Seq<>();
    private static final Seq<Cons<HaoNetPackage>> clientListen = new Seq<>();
    private static final Seq<Cons2<NetConnection, HaoNetPackage>> serverListen = new Seq<>();
    private static Task authSendTask;
    private static Task authTimeoutTask;
    private static boolean isClient = true;
    private static boolean isHost = false;
    private static boolean isSinglePlayer = true;
    protected static boolean already = false;
    protected static boolean isConnecting = false;
    protected static boolean isGameOver = false;
    protected static boolean hasThisMod = true;

    public static class ServerStateChange {
        public final boolean hasThisMod;
        public ServerStateChange(boolean hasMod) {
            this.hasThisMod = hasMod;
        }
    }

    public static void onChange(boolean hasMod) {
        hasThisMod = hasMod;
        Events.fire(new ServerStateChange(hasMod));
    }

    private static void handleClient(HaoNetPackage packet) {
        try {
            for (Cons<HaoNetPackage> listener : clientListen) {
                listener.get(packet);
            }
        } catch (Throwable e) {
            Log.err(e);
        }
    }

    private static void handleServer(NetConnection connection, HaoNetPackage packet) {
        try {
            for (Cons2<NetConnection, HaoNetPackage> listener : serverListen) {
                listener.get(connection, packet);
            }
        } catch (Throwable e) {
            Log.err(e);
        }
    }

    public static void addHandleClient(Cons<HaoNetPackage> cons) {
        clientListen.add(cons);
    }

    public static void addHandleServer(Cons2<NetConnection, HaoNetPackage> cons) {
        serverListen.add(cons);
    }

    public static boolean isClient() {
        return isClient;
    }

    public static boolean isHost() {
        return isHost;
    }

    public static boolean isSinglePlayer() {
        return isSinglePlayer;
    }

    public static boolean hasMod() {
        return hasThisMod;
    }

    public static void post(HaoNetPackage pack) {
        net.send(pack, true);
    }

    public static void load() {
        if (!ENABLE) return;

        // Server side handler for general packets
        net.handleServer(HaoNetPackage.class, Server::handleServer);

        // Client side handler for general packets
        net.handleClient(HaoNetPackage.class, packet -> {
            if (authSendTask != null) {
                Timer.schedule(() -> {
                    Log.info("Auth send task cancelled");
                    authSendTask.cancel();
                    authSendTask = null;
                }, WAIT_TIME * 1.2f);
            }

            if (packet.getPriority() == Packet.priorityHigh) {
                handleClient(packet);
            } else {
                clientQueue.add(packet);
            }
        });

        // Server side auth handler
        net.handleServer(HaoNetPackageClient.class, (connection, packet) -> {
            if (!playerConnections.containsKey(connection)) return;

            Log.info("[green][Server][][blue] Received auth packet from uuid '[accent]@[]'", connection.uuid);
            if (!packet.name.equals(Main.name)) return;

            if (!packet.version.equals(Main.version)) {
                connection.kick("Mod version required: [accent]" + Main.version + "[]. Your version: [accent]" + packet.version + "[]");
                return;
            }

            playerConnections.remove(connection);
            players.add(connection);

            HaoNetPackageClient response = new HaoNetPackageClient();
            connection.send(response, true);
            Log.info("[green][Server][][blue] Auth completed for uuid '[accent]@[]'", connection.uuid);
        });

        // Client side auth completion handler
        net.handleClient(HaoNetPackageClient.class, con -> {
            onChange(con.name.equals(Main.name));
            Log.info("[red][Client][green] Auth successful");
            isConnecting = false;
            authTimeoutTask.cancel();
        });

        // Server: Start auth on connection
        Events.on(EventType.ConnectPacketEvent.class, e -> {
            Log.info("[green][Server][][blue] Auth started for uuid '[accent]@[]'", e.connection.uuid);
            playerConnections.put(e.connection, 0);
        });

        // Client: Detect connection start
        Events.on(ClientServerConnectEvent.class, e -> {
            if (isGameOver) {
                isGameOver = false;
                return;
            }
            isConnecting = true;
            isClient = true;
            isSinglePlayer = false;
            isHost = false;
        });

        Events.on(EventType.GameOverEvent.class, e -> {
            isGameOver = true;
        });

        // Client: Send auth on world load
        Events.on(EventType.WorldLoadEndEvent.class, e -> {
            if (!isConnecting) return;

            Log.info("[red][Client][blue] Sending auth packet to server...");

            authSendTask = Timer.schedule(() -> {
                HaoNetPackageClient p = new HaoNetPackageClient();
                Core.app.post(() -> net.send(p, false));
            }, 1f, 0.5f);

            authTimeoutTask = Timer.schedule(() -> {
                Log.info("[red][Client][scarlet] Auth failed.");
                onChange(false);
                isConnecting = false;
                if (authSendTask != null) {
                    authSendTask.cancel();
                    authSendTask = null;
                }
                Log.info("Auth send task cancelled due to timeout");
            }, WAIT_TIME * 1.25f);
        });

        Events.on(HostEvent.class, e -> {
            isHost = true;
            isSinglePlayer = false;
            isConnecting = false;
            isClient = false;
        });
    }

    public static void interval() {
        if (already || !ENABLE) return;
        already = true;

        Timer.schedule(() -> {
            // Update client/host state
            isClient = !net.server();

            if (isClient) {
                for (HaoNetPackage packet : clientQueue) {
                    handleClient(packet);
                }
                clientQueue.clear();
                return;
            }

            // Process auth timeouts safely
            Seq<NetConnection> connections = playerConnections.keys().toArray();
            Seq<NetConnection> toRemove = new Seq<>();
            Seq<NetConnection> toKick = new Seq<>();
            ObjectIntMap<NetConnection> toIncrement = new ObjectIntMap<>();

            for (NetConnection con : connections) {
                int value = playerConnections.get(con);
                if (con.hasDisconnected) {
                    toRemove.add(con);
                    continue;
                }
                if (value > AUTH_TIMEOUT_TICKS && con.hasConnected) {
                    toKick.add(con);
                    continue;
                }
                toIncrement.put(con, value + 1);
            }

            for (NetConnection con : toRemove) {
                playerConnections.remove(con);
            }

            for (NetConnection con : toKick) {
                playerConnections.remove(con);
                con.kick("[scarlet]Connection couldn't be authenticated.[]\n\nThis server requires [accent][Better Vanilla] - (" + Main.version + ")[] to play!", 10);
            }

            for (Entry<NetConnection> entry : toIncrement.entries()) {
                playerConnections.put(entry.key, entry.value);
            }

            // Broadcast queued packets
            if (sender.size > 0) {
                HaoNetPackage pak = sender.get(0);
                sender.remove(0);
                forEachPlayer(player -> player.send(pak, true));
            }
        }, 0f, 0.25f);
    }

    public static void forEachPlayer(Cons<NetConnection> cons) {
        for (int i = 0; i < players.size; i++) {
            NetConnection con = players.get(i);
            if (con.hasDisconnected) {
                players.remove(i);
                i--;
                continue;
            }
            cons.get(con);
        }
    }
}