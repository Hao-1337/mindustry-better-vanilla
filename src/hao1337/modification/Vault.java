package hao1337.modification;

import arc.Core;
import mindustry.Vars;
import mindustry.world.blocks.storage.StorageBlock;

public class Vault {
    private static int capacity1;
    private static int capacity2;
    private static boolean coreMerge = false;
    private static StorageBlock vault;
    private static StorageBlock reinforcedContainer;
    private static StorageBlock reinforcedVault;
    private static boolean ik;

    public static int newCapacity1 = 3000;
    public static int newCapacity2 = 1500;
    public static boolean newCoreMerge = true;

    public static void load() {
        vault = (StorageBlock) Vars.content.block("vault");
        reinforcedVault = (StorageBlock) Vars.content.block("reinforced-vault");
        reinforcedContainer = (StorageBlock) Vars.content.block("reinforced-container");

        capacity1 = vault.itemCapacity;
        capacity2 = reinforcedVault.itemCapacity;
        coreMerge = reinforcedVault.coreMerge;

        ik = Core.settings.getBool("hao1337.gameplay.vault-bigger");
        apply(true);
    }

    public static void apply(boolean is) {
        if (is && ik) {
            vault.itemCapacity = newCapacity1;
            reinforcedVault.itemCapacity = newCapacity2;
            reinforcedVault.coreMerge = reinforcedContainer.coreMerge = newCoreMerge;
            return;
        }
        vault.itemCapacity = capacity1;
        reinforcedVault.itemCapacity = capacity2;
        reinforcedVault.coreMerge = reinforcedContainer.coreMerge = coreMerge;
    }
}
