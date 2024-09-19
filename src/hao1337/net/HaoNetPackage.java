package hao1337.net;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

public class HaoNetPackage extends Packet {
    public boolean preventLoop = true;
    public String string = "";
    public int tcSpeed = 1;
    public boolean tcEnable = true;

    @Override
    public int getPriority() {
        return Packet.priorityLow;
    }

    @Override
    public void write(Writes writer) {
        writer.bool(preventLoop);
        writer.str(string);
        writer.i(tcSpeed);
        writer.bool(tcEnable);
    }

    @Override
    public void read(Reads reader) {
        preventLoop = reader.bool();
        string = reader.str();
        tcSpeed = reader.i();
        tcEnable = reader.bool();
    }
}
