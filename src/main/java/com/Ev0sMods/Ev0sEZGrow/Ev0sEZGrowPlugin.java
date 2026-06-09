package com.Ev0sMods.Ev0sEZGrow;

import com.Ev0sMods.Ev0sEZGrow.player.CrouchGrowerSystem;
import com.Ev0sMods.Ev0sEZGrow.player.CrouchTrackerAddSystem;
import com.Ev0sMods.Ev0sEZGrow.player.CrouchTrackerComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class Ev0sEZGrowPlugin extends JavaPlugin {

    public Ev0sEZGrowPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        System.out.println("[Ev0sEZGrow] Plugin loaded!");
    }

    @Override
    protected void setup() {
        super.setup();
        EzGrowConfig.load();
        System.out.println("[Ev0sEZGrow] Plugin enabled!");

        var esr = this.getEntityStoreRegistry();

        CrouchTrackerComponent.COMPONENT_TYPE = esr.registerComponent(
                CrouchTrackerComponent.class,
                "CrouchTracker",
                CrouchTrackerComponent.CODEC);

        esr.registerSystem(new CrouchTrackerAddSystem());
        esr.registerSystem(new CrouchGrowerSystem(CrouchTrackerComponent.COMPONENT_TYPE));
    }

    public void onDisable() {
        System.out.println("[Ev0sEZGrow] Plugin disabled!");
    }
}
