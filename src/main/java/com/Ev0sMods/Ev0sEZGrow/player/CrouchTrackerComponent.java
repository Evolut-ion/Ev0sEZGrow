package com.Ev0sMods.Ev0sEZGrow.player;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Per-player transient component that tracks the previous crouch state
 * so the CrouchGrowerSystem can detect the not-crouching → crouching edge.
 */
public class CrouchTrackerComponent implements Component<EntityStore> {

    public static ComponentType<EntityStore, CrouchTrackerComponent> COMPONENT_TYPE;

    public static final BuilderCodec<CrouchTrackerComponent> CODEC =
            BuilderCodec.builder(CrouchTrackerComponent.class, CrouchTrackerComponent::new).build();

    /** Whether the player was crouching on the previous tick. */
    public boolean wasCrouching = false;

    public CrouchTrackerComponent() {}

    private CrouchTrackerComponent(CrouchTrackerComponent other) {
        this.wasCrouching = other.wasCrouching;
    }

    @Override
    public CrouchTrackerComponent clone() {
        return new CrouchTrackerComponent(this);
    }
}
