package hao1337.net;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

public class ModStatePackage extends Packet {
    public boolean thoriumConveyor;
    public boolean surgeConveyor;

    public boolean box;
    public boolean silo;
    public boolean ultraVault;
    public boolean valveUnloader;
    public boolean leviathanReconstructor;

    public boolean dropper;
    public boolean m1014;

    public boolean giganticDome;
    public boolean slagCentrifuge;
    public boolean heatReactor;

    public boolean betterShield;
    public boolean betterOverrideDome;
    public boolean betterSerpuloVault;
    public boolean betterErekirVault;

    public boolean hiddenLiquids;
    public boolean hiddenItems;

    public int sechematicSize;
    public boolean experimental;

    
    @Override
    public int getPriority() {
        return Packet.priorityLow;
    }

    @Override
    public void write(Writes writer) {
        writer.bool(thoriumConveyor);
        writer.bool(surgeConveyor);
    
        writer.bool(box);
        writer.bool(silo);
        writer.bool(ultraVault);
        writer.bool(valveUnloader);
        writer.bool(leviathanReconstructor);
    
        writer.bool(dropper);
        writer.bool(m1014);
    
        writer.bool(giganticDome);
        writer.bool(slagCentrifuge);
        writer.bool(heatReactor);
    
        writer.bool(betterShield);
        writer.bool(betterOverrideDome);
        writer.bool(betterSerpuloVault);
        writer.bool(betterErekirVault);
    
        writer.bool(hiddenLiquids);
        writer.bool(hiddenItems);
    
        writer.i(sechematicSize);
        writer.bool(experimental);
    }

    @Override
    public void read(Reads reader) {
        thoriumConveyor = reader.bool();
        surgeConveyor = reader.bool();

        box = reader.bool();
        silo = reader.bool();
        ultraVault = reader.bool();
        valveUnloader = reader.bool();
        leviathanReconstructor = reader.bool();

        dropper = reader.bool();
        m1014 = reader.bool();

        giganticDome = reader.bool();
        slagCentrifuge = reader.bool();
        heatReactor = reader.bool();

        betterShield = reader.bool();
        betterOverrideDome = reader.bool();
        betterSerpuloVault = reader.bool();
        betterErekirVault = reader.bool();

        hiddenLiquids = reader.bool();
        hiddenItems = reader.bool();

        sechematicSize = reader.i();
        experimental = reader.bool();
    }
}
