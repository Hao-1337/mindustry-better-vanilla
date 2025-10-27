package hao1337.modification;

import arc.struct.Seq;
import mindustry.content.TechTree;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Objectives;
import mindustry.type.ItemStack;

public class TechTreeModification {
    public static void margeNode(UnlockableContent parent, UnlockableContent child) {
        TechTree.all.find((t) -> t.content == parent).children.add(TechTree.node(child));
    }

    public static void margeNode(UnlockableContent parent, UnlockableContent child, ItemStack[] requirements) {
        TechTree.all.find((t) -> t.content == parent).children.add(TechTree.node(child, requirements, (Seq<Objectives.Objective>) null, () -> {}));
    }

    public static void margeNode(UnlockableContent parent, UnlockableContent child, ItemStack[] requirements, Seq<Objectives.Objective> objectives) {
        TechTree.all.find((t) -> t.content == parent).children.add(TechTree.node(child, objectives, () -> {}));
    }

    public static void margeNode(UnlockableContent parent, UnlockableContent child, Seq<Objectives.Objective> objectives) {
        TechTree.all.find((t) -> t.content == parent).children.add(TechTree.node(child, child.researchRequirements(), objectives, () -> {}));
    }

    // @SuppressWarnings("deprecation")
    public static void margeNodeProduce(UnlockableContent parent, UnlockableContent child, int index) {
        // Yeah it gone!
        // `Caused by: java.lang.NoSuchMethodError: 'arc.struct.Seq arc.struct.Seq.filter(arc.func.Boolf)'`
        // TechTree.all.filter(t -> t.content == parent).get(index).children.add(TechTree.nodeProduce(child, () -> {}));
        Seq<TechTree.TechNode> filtered = new Seq<>(TechTree.TechNode.class);
        for (Object o : TechTree.all) {
            if (o instanceof TechTree.TechNode t && t.content == parent) {
                filtered.add(t);
            }
        }

        if (index < 0 || index >= filtered.size) return; 
        TechTree.TechNode target = filtered.get(index);
        target.children.add(TechTree.nodeProduce(child, () -> {}));
    }
}
