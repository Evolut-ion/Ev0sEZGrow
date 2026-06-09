package com.Ev0sMods.Ev0sEZGrow.player;

import com.Ev0sMods.Ev0sEZGrow.EzGrowConfig;
import com.Ev0sMods.Ev0sEZGrow.util.GrowthUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Runs every tick for each player.  On the not-crouching → crouching transition
 * it fires GrowthUtil to advance all crops and saplings within radius blocks.
 */
public class CrouchGrowerSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, Player> playerType;
    private final ComponentType<EntityStore, MovementStatesComponent> movementType;
    private final ComponentType<EntityStore, TransformComponent> transformType;
    private final ComponentType<EntityStore, CrouchTrackerComponent> trackerType;
    private final Query<EntityStore> query;

    public CrouchGrowerSystem(ComponentType<EntityStore, CrouchTrackerComponent> trackerType) {
        this.trackerType   = trackerType;
        this.playerType    = Player.getComponentType();
        this.movementType  = MovementStatesComponent.getComponentType();
        this.transformType = TransformComponent.getComponentType();
        this.query = Query.and(playerType, movementType, transformType, trackerType);
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        MovementStatesComponent movComp   = archetypeChunk.getComponent(index, movementType);
        TransformComponent      transform = archetypeChunk.getComponent(index, transformType);
        CrouchTrackerComponent  tracker   = archetypeChunk.getComponent(index, trackerType);

        if (movComp == null || transform == null || tracker == null) return;

        MovementStates states = movComp.getMovementStates();
        boolean crouching = states.crouching;

        // Only fire on the leading edge of a crouch press (not held)
        if (crouching && !tracker.wasCrouching) {
            World world = store.getExternalData().getWorld();
            if (world != null) {
                Vector3d pos = transform.getPosition();
                int px = (int) Math.floor(pos.x);
                int py = (int) Math.floor(pos.y);
                int pz = (int) Math.floor(pos.z);

                int advanced = GrowthUtil.applyGrowthInRadius(world, px, py, pz, EzGrowConfig.radius, store);

                if (advanced > 0 && EzGrowConfig.consumeFertilizer) {
                    Player player = archetypeChunk.getComponent(index, playerType);
                    if (player != null) {
                        consumeFertilizer(player.getInventory().getHotbar(), advanced);
                    }
                }
            }
        }

        tracker.wasCrouching = crouching;
    }

    @SuppressWarnings("removal")
    private static void consumeFertilizer(ItemContainer hotbar, int count) {
        for (int i = 0; i < count; i++) {
            for (short slot = 0; slot < 9; slot++) {
                try {
                    ItemStack stack = hotbar.getItemStack(slot);
                    if (stack == null || !isFertilizer(stack.getItemId())) continue;
                    hotbar.removeItemStackFromSlot(slot, 1);
                    break;
                } catch (Exception ignored) {}
            }
        }
    }

    private static boolean isFertilizer(String itemId) {
        if (itemId == null) return false;
        String id = itemId.toLowerCase();
        return id.contains("fertil")
            || itemId.equals("Tool_Compost")
            || itemId.equals("Tool_Super_Compost")
            || itemId.equals("Tool_Ultra_Compost");
    }
}
