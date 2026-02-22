package hao1337.net;

import mindustry.Vars;

public enum ProtocolType {
    // Pure server (run from server jar)
    SERVER(3),
    // Current user join multi player game
    CLIENT(2),
    // Server, but player base host (run from client jar)
    HOST(1),
    // Player just play in single player mode
    LOCAL(0);

    public final int protocolId;

    ProtocolType(int id) {
        this.protocolId = id;
    }

    public static ProtocolType fetch() {
        boolean active = Vars.net.active(),
                server = Vars.net.server(),
                client = Vars.net.client();

        if (active == false && client == false)
            return server ? SERVER : LOCAL;
        if (active == true && server == true)
            return HOST;
        if (active == true && client == true)
            return CLIENT;
        return LOCAL;
    }

    @Override
    public String toString() {
        return switch (this) {
            case LOCAL -> "Single player";
            case CLIENT -> "Client";
            case HOST -> "Player-host server";
            case SERVER -> "Dedicated server";
        };
    }

    public boolean isServer() {
        return this == SERVER || this == HOST;
    }
}
