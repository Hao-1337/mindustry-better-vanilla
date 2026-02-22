package hao1337.net;

public class Net {
    static {
        mindustry.net.Net.registerPacket(IOPacket::new);
        mindustry.net.Net.registerPacket(HandShakePacket::new); 
    }

    public static final Protocol protocol = new Protocol();
    public static final IORouter router = new IORouter();

    public Net() { init(); }

    public boolean isSinglePlayer() {
        return protocol.lastProtocolType == ProtocolType.LOCAL;
    }

    public void init() {
        protocol.init();
        router.init();
    }
}
