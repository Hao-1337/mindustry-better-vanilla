package hao1337.modification;

import arc.Core;
import mindustry.content.Blocks;
import mindustry.world.blocks.storage.StorageBlock;

public class Vault {
    private static int capacity1;
    private static int capacity2;
    private static boolean coreMerge = false;
    private static boolean ik;
    private static boolean ik1;

    public static int newCapacity1 = 3000;
    public static int newCapacity2 = 1500;
    public static boolean newCoreMerge = true;

    public static void load() {
        capacity1 = ((StorageBlock)Blocks.vault).itemCapacity;
        capacity2 = ((StorageBlock)Blocks.reinforcedVault).itemCapacity;
        coreMerge = ((StorageBlock)Blocks.reinforcedVault).coreMerge;

        ik = Core.settings.getBool("hao1337.gameplay.serpulo.vault-bigger");
        ik1 = Core.settings.getBool("hao1337.gameplay.erekir.vault-bigger");
        apply(true);
    }

    public static void apply(boolean is) {
        if (is && ik) {
            ((StorageBlock)Blocks.vault).itemCapacity = newCapacity1;
            return;
        }
        else ((StorageBlock)Blocks.vault).itemCapacity = capacity1;
        if (is && ik1) {
            ((StorageBlock)Blocks.reinforcedVault).itemCapacity = newCapacity2;
            ((StorageBlock)Blocks.reinforcedVault).coreMerge = ((StorageBlock)Blocks.reinforcedContainer).coreMerge = newCoreMerge;
            return;
        }
        else {
            ((StorageBlock)Blocks.reinforcedVault).itemCapacity = capacity2;
            ((StorageBlock)Blocks.reinforcedVault).coreMerge = ((StorageBlock)Blocks.reinforcedContainer).coreMerge = coreMerge;
        }
    }
}
