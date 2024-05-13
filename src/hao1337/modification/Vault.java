package hao1337.modification;

import arc.Core;
import mindustry.Vars;
import mindustry.world.blocks.storage.StorageBlock;

public class Vault {
    public static void load() {
        if (Core.settings.getBool("hao1337.gameplay.vault-bigger")) {
            StorageBlock vault = (StorageBlock)Vars.content.block("vault");
            StorageBlock reinforcedVault = (StorageBlock)Vars.content.block("reinforced-vault");
            StorageBlock reinforcedContainer = (StorageBlock)Vars.content.block("reinforced-container");

            vault.itemCapacity = 3000;
            reinforcedVault.itemCapacity = 2000;
            reinforcedVault.coreMerge = reinforcedContainer.coreMerge = true;
        }
    }
}
