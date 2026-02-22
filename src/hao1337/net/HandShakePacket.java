package hao1337.net;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

public class HandShakePacket extends Packet {
    public String name;
    public String version;

    public HandShakePacket(){}

    public HandShakePacket(String name, String version){
        this.name = name;
        this.version = version;
    }

    @Override
    public void write(Writes w){
        w.str(name);
        w.str(version);
    }

    @Override
    public void read(Reads r){
        name = r.str();
        version = r.str();
    }

    @Override
    public int getPriority(){
        return Packet.priorityHigh;
    }
} 