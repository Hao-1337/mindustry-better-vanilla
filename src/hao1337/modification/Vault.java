package hao1337.modification;

import mindustry.content.Blocks;
import mindustry.world.blocks.storage.StorageBlock;

public class Vault {
    private static int capacity1;
    private static int capacity2;
    private static boolean coreMerge = false;

    public static int newCapacity1 = 3000;
    public static int newCapacity2 = 1500;
    public static boolean newCoreMerge = true;

    public static void load() {
        capacity1 = ((StorageBlock)Blocks.vault).itemCapacity;
        capacity2 = ((StorageBlock)Blocks.reinforcedVault).itemCapacity;
        coreMerge = ((StorageBlock)Blocks.reinforcedVault).coreMerge;
    }

    public static void apply(boolean serpulo, boolean erekir) {
        if (serpulo) {
            ((StorageBlock) Blocks.vault).itemCapacity = newCapacity1;
        } else
            ((StorageBlock) Blocks.vault).itemCapacity = capacity1;
        if (erekir) {
            ((StorageBlock) Blocks.reinforcedVault).itemCapacity = newCapacity2;
            ((StorageBlock) Blocks.reinforcedVault).coreMerge = ((StorageBlock) Blocks.reinforcedContainer).coreMerge = newCoreMerge;
        } else {
            ((StorageBlock) Blocks.reinforcedVault).itemCapacity = capacity2;
            ((StorageBlock) Blocks.reinforcedVault).coreMerge = ((StorageBlock) Blocks.reinforcedContainer).coreMerge = coreMerge;
        }
    }
}
