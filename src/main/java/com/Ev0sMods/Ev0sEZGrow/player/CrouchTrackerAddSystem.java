package com.Ev0sMods.Ev0sEZGrow.player;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Ensures every player entity gets a CrouchTrackerComponent when it is added to the EntityStore.
 */
public class CrouchTrackerAddSystem extends HolderSystem<EntityStore> {

    private final Query<EntityStore> query;

    public CrouchTrackerAddSystem() {
        this.query = Player.getComponentType();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onEntityAdd(@Nonnull Holder<EntityStore> holder, @Nonnull AddReason reason,
                            @Nonnull Store<EntityStore> store) {
        holder.ensureComponent(CrouchTrackerComponent.COMPONENT_TYPE);
    }

    @Override
    public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason,
                                @Nonnull Store<EntityStore> store) {
        // Component is removed automatically with the entity — nothing to do.
    }
}
