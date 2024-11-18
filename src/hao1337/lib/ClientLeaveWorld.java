package hao1337.lib;

import arc.Events;
import arc.scene.ui.layout.Table;
import mindustry.Vars;

public class ClientLeaveWorld {
    public static class ClientLeaveWorldEvent {}
    public static boolean isChange = true;

    public static void load() {
        Table menuT = new Table();

        menuT.update(() -> {
            if (isChange) return;
            isChange = true;
            Events.fire(new ClientLeaveWorldEvent());
        });

        Vars.ui.menuGroup.addChild(menuT);

        Table hudT = new Table();
        hudT.update(() -> {
            if (!isChange) return;
            isChange = false;
        });

        Vars.ui.hudGroup.addChild(hudT);
    }
}
