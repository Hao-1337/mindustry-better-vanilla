package hao1337.net;

import arc.util.io.Reads;
import arc.util.io.Writes;
import hao1337.Main;
import mindustry.net.Packet;

public class HaoNetPackageClient extends Packet{
    public String name = Main.name;
    public String version = Main.version;

    @Override
    public int getPriority() {
        return Packet.priorityHigh;
    }

    @Override
    public void write(Writes writer) {
        writer.str(name);
        writer.str(version);
    }

    @Override
    public void read(Reads reader) {
        name = reader.str();
        version = reader.str();
    }
}
