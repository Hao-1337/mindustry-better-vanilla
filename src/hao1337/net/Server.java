package hao1337.net;

import static mindustry.Vars.*;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Cons2;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectIntMap.Entry;
import arc.struct.ObjectIntMap.Entries;
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
    public static final float waitTime = 2f;
    public static final boolean enable = true;

    private static ObjectIntMap<NetConnection> playerConnections = new ObjectIntMap<>();
    private static Seq<NetConnection> players = new Seq<>();
    private static Seq<HaoNetPackage> clientQueue = new Seq<>();
    private static Seq<HaoNetPackage> sender = new Seq<>();
    private static Seq<Cons<HaoNetPackage>> clientListen = new Seq<>();
    private static Seq<Cons2<NetConnection, HaoNetPackage>> serverListen = new Seq<>();
    private static Task task, task1;
    private static boolean isClient = true;
    private static boolean isHost = false;
    private static boolean isSinglePlayer = true;
    protected static boolean already = false;
    protected static boolean isConnecting = false;
    protected static boolean isGameOver = false;
    protected static boolean hasThisMod = true;

    public static class ServerStateChange {
        public boolean hasThisMod = false;
        public ServerStateChange(boolean ne) {
            hasThisMod = ne;
        }
    }

    public static void onChange(boolean c) {
        hasThisMod = c;
        Events.fire(new ServerStateChange(c));
    }

    static void handleClient(HaoNetPackage packet) {
        try {
            for (Cons<HaoNetPackage> c : clientListen)
                c.get(packet);
        } catch (Throwable e) {
            Log.err(e);
        }
    }

    static void handleServer(NetConnection connection, HaoNetPackage packet) {
        try {
            for (Cons2<NetConnection, HaoNetPackage> c : serverListen)
                c.get(connection, packet);
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
        if (!enable)
            return;

        //* {Server} Server side handler
        net.handleServer(HaoNetPackage.class, new Cons2<NetConnection, HaoNetPackage>() {
            @Override
            public void get(NetConnection connection, HaoNetPackage packet) {
                handleServer(connection, packet);
            }
        });

        //* {Client} Client side handler
        net.handleClient(HaoNetPackage.class, new Cons<HaoNetPackage>() {
            @Override
            public void get(HaoNetPackage packet) {
                if (task != null)
                    task.cancel();

                if (packet.getPriority() == Packet.priorityHigh)
                    handleClient(packet);
                else
                    clientQueue.add(packet);
            }
        });


        //* {Server} Auth on server side
        net.handleServer(HaoNetPackageClient.class, new Cons2<NetConnection, HaoNetPackageClient>() {
            @Override
            public void get(NetConnection connection, HaoNetPackageClient packet) {
                if (!playerConnections.containsKey(connection))
                    return;

                Log.info("[green][Server][][blue] Get auth packet from uuid '[accent]@[blue]'", connection.uuid);
                if (!packet.name.equals(Main.name))
                    return;

                if (!packet.version.equals(Main.version)) {
                    connection.kick("Mod version request: [accent]" + Main.version + "[]. You version is: [accent]"
                            + packet.version + "[]");
                    return;
                }

                playerConnections.remove(connection);
                players.add(connection);

                HaoNetPackageClient p = new HaoNetPackageClient();
                connection.send(p, true);
                Log.info("[green][Server][][blue] Auth complete for uuid '[accent]@[blue]'", connection.uuid);
            }
        });

        //* {Client} Complete auth by sending to client complete auth packet
        net.handleClient(HaoNetPackageClient.class, new Cons<HaoNetPackageClient>() {
            @Override
            public void get(HaoNetPackageClient con) {
                onChange(con.name.equals(Main.name));
                Log.info("[red][Client][green] Auth succesful");
                isConnecting = false;
                task1.cancel();
            }
        });

        //* {Server} Server will store this connection and wait for a auth package
        Events.on(EventType.ConnectPacketEvent.class, e -> {
            Log.info("[green][Server][][blue] Auth for uuid '[accent]@[blue]' started", e.connection.address);

            try {
                playerConnections.put(e.connection, 0);
            } catch (Throwable c) {}
        });

        //* {Client} Check if this is connection event is publish, not a world load event, it jsut help you skip the wait for the bad network
        Events.on(ClientServerConnectEvent.class, e -> {
            // Log.info("Connect event");
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
            // Log.info("Gameover event");
            isGameOver = true;
        });

        //* {Client} Try to send auth package
        Events.on(EventType.WorldLoadEndEvent.class, e -> {
            // Log.info("World load end event");
            if (!isConnecting) return;
    
            Log.info("[red][Client][blue] Now sending auth packet to server...");

            task = Timer.schedule(() -> {
                // if (!netClient.isConnecting()) return;
                HaoNetPackageClient p = new HaoNetPackageClient();

                Core.app.post(() -> net.send(p, false));
            }, .2f, .33f);

            task1 = Timer.schedule(() -> {
                Log.info("[red][Client][scarlet] Auth failed.");
                onChange(false);
                isConnecting = false;
                task.cancel();
            }, waitTime * 1.25f);
        });

        Events.on(HostEvent.class, e -> {
            isHost = true;
            isSinglePlayer = false;
            isConnecting = false;
            isClient = false;
        });
    }

    public static void interval() {
        if (already || !enable)
            return;
        already = true;

        Timer.schedule(() -> {
            // Check if any host here
            isClient = !net.server();

            if (isClient) {
                for (HaoNetPackage packet : clientQueue) {
                    handleClient(packet);
                }
                clientQueue.clear();
                return;
            }

            // Auth timeout
            Entries<NetConnection> queue = playerConnections.entries();
            while (queue.hasNext()) {
                Entry<NetConnection> cur = queue.next();
                if (cur.key.hasDisconnected) {
                    playerConnections.remove(cur.key);
                    continue;
                }

                if (cur.value > waitTime * 4 && cur.key.hasConnected) {
                    playerConnections.remove(cur.key);
                    cur.key.kick(
                            "[scarlet]Connection couldn't be authenticated.[]\n\nThis server need you to install [accent][Better Vanilla] - ("
                                    + Main.version + ")[] to play!",
                            10);
                    continue;
                }

                playerConnections.remove(cur.key);
                playerConnections.put(cur.key, cur.value + 1);
            }

            // Send data packet to client
            if (!isClient && sender.size > 0) {
                HaoNetPackage pak = sender.get(0);
                sender.remove(0);

                forEachPlayer(player -> player.send(pak, true));
            }
        }, 0f, 0.25f);
    }

    /**
     * Run action on every player
     * @param cons Function that should loop to every player
     */
    public static void forEachPlayer(Cons<NetConnection> cons) {
        for (NetConnection con : players) {
            if (con.hasDisconnected) {
                players.remove(players.indexOf(con));
                continue;
            }
            cons.get(con);
        }
    }
}
