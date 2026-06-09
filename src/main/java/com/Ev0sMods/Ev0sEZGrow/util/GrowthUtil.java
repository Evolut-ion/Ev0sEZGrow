package com.Ev0sMods.Ev0sEZGrow.util;

import com.hypixel.hytale.builtin.adventure.farming.config.stages.PrefabFarmingStageData;
import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingData;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingStageData;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;

public final class GrowthUtil {

    private GrowthUtil() {}

    /**
     * Scans all blocks within {@code radius} of (px, py, pz) and advances
     * any crops or saplings found by one growth stage.  Sparkle particles
     * appear at each block that is successfully advanced.
     */
    public static int applyGrowthInRadius(World world, int px, int py, int pz,
                                          int radius, Store<EntityStore> entityAccessor) {
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

        HashMap<Long, WorldChunk> chunkCache         = new HashMap<>();
        HashMap<Long, Ref<ChunkStore>> chunkRefCache = new HashMap<>();
        HashMap<Long, BlockComponentChunk> bccCache  = new HashMap<>();
        HashMap<Long, Ref<ChunkStore>> sectionRefCache = new HashMap<>();

        int advanced = 0;
        int rSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx*dx + dy*dy + dz*dz > rSq) continue;
                    if (tryAdvanceBlock(world, px + dx, py + dy, pz + dz,
                            chunkStore, entityAccessor,
                            chunkCache, chunkRefCache, bccCache, sectionRefCache)) {
                        advanced++;
                    }
                }
            }
        }
        return advanced;
    }

    @SuppressWarnings("unchecked")
    private static boolean tryAdvanceBlock(
            World world, int x, int y, int z,
            Store<ChunkStore> chunkStore,
            Store<EntityStore> entityAccessor,
            HashMap<Long, WorldChunk> chunkCache,
            HashMap<Long, Ref<ChunkStore>> chunkRefCache,
            HashMap<Long, BlockComponentChunk> bccCache,
            HashMap<Long, Ref<ChunkStore>> sectionRefCache) {
        try {
            long chunkIdx = ChunkUtil.indexChunkFromBlock(x, z);

            WorldChunk chunk;
            if (!chunkCache.containsKey(chunkIdx)) {
                chunk = world.getChunkIfInMemory(chunkIdx);
                chunkCache.put(chunkIdx, chunk);
            } else {
                chunk = chunkCache.get(chunkIdx);
            }
            if (chunk == null) return false;

            BlockType blockType = VectorCompat.getBlockType(chunk, x, y, z);
            if (blockType == null) { System.out.println("[EZGrow] bail: blockType null at " + x + "," + y + "," + z); return false; }
            String bid = blockType.getId();
            if (bid.equals("Empty") || bid.equals("Air")) return false;

            // Tree-part blocks (branch/log/trunk) appear at the sapling's origin
            // position after a PrefabFarmingStageData stage fires.  Include them
            // so the ID gate doesn't block multi-stage tree advancement.
            boolean isSapling = bid.contains("Plant_Sapling_") || bid.contains("_Sapling")
                             || bid.contains("_Branch") || bid.contains("_Log") || bid.contains("_Trunk");
            boolean isCrop    = bid.contains("Plant_Crop_") || bid.contains("Plant_Seeds_");
            if (!isSapling && !isCrop) return false;

            // Resolve bcc/blockRef/farmingBlock early so we can fall back to
            // getPreviousBlockType() when the current block has no farming data.
            Ref<ChunkStore> chunkRef;
            if (!chunkRefCache.containsKey(chunkIdx)) {
                chunkRef = world.getChunkStore().getChunkReference(chunkIdx);
                chunkRefCache.put(chunkIdx, chunkRef);
            } else {
                chunkRef = chunkRefCache.get(chunkIdx);
            }
            if (chunkRef == null) { System.out.println("[EZGrow] bail: chunkRef null at " + x + "," + y + "," + z); return false; }

            BlockComponentChunk bcc;
            if (!bccCache.containsKey(chunkIdx)) {
                bcc = (BlockComponentChunk) chunkStore.getComponent(chunkRef, BlockComponentChunk.getComponentType());
                bccCache.put(chunkIdx, bcc);
            } else {
                bcc = bccCache.get(chunkIdx);
            }
            if (bcc == null) { System.out.println("[EZGrow] bail: bcc null at " + x + "," + y + "," + z); return false; }

            int blockIndexColumn = ChunkUtil.indexBlockInColumn(x, y, z);
            Ref<ChunkStore> blockRef = bcc.getEntityReference(blockIndexColumn);
            if (blockRef == null) { System.out.println("[EZGrow] bail: blockRef null bid=" + bid + " at " + x + "," + y + "," + z); return false; }

            FarmingBlock farmingBlock = (FarmingBlock) chunkStore.getComponent(blockRef, FarmingBlock.getComponentType());
            if (farmingBlock == null) { System.out.println("[EZGrow] bail: farmingBlock null bid=" + bid + " at " + x + "," + y + "," + z); return false; }

            FarmingData farmingData = blockType.getFarming();
            if (farmingData == null) {
                String prevId = farmingBlock.getPreviousBlockType();
                if (prevId != null) {
                    BlockType origType = (BlockType) BlockType.getAssetMap().getAsset(prevId);
                    if (origType != null) farmingData = origType.getFarming();
                }
                // State-qualified block IDs (e.g. *Plant_Crop_Chilli_Block_Stage1) may not
                // carry farming data themselves — strip the asterisk and progressively
                // remove state suffixes to find the base block type that does.
                if (farmingData == null && bid.startsWith("*")) {
                    String base = bid.substring(1);
                    while (farmingData == null && base.contains("_")) {
                        BlockType bt = (BlockType) BlockType.getAssetMap().getAsset(base);
                        if (bt != null) farmingData = bt.getFarming();
                        if (farmingData == null) base = base.substring(0, base.lastIndexOf('_'));
                    }
                }
                if (farmingData == null) {
                    try { chunk.setTicking(x, y, z, true); } catch (Exception ignored) {}
                    spawnSparkle(x, y, z, entityAccessor);
                    return false;
                }
            }

            if (farmingData.getStages() == null) return false;
            String stageSetName = farmingBlock.getCurrentStageSet();
            if (stageSetName == null) stageSetName = farmingData.getStartingStageSet();

            FarmingStageData[] stages = (FarmingStageData[]) farmingData.getStages().get(stageSetName);
            if (stages == null || stages.length == 0) return false;

            int currentStage = (int) farmingBlock.getGrowthProgress();
            int targetStage  = Math.min(currentStage + 1, stages.length - 1);
            if (targetStage <= currentStage) return false;

            int sx = ChunkUtil.chunkCoordinate(x);
            int sy = ChunkUtil.chunkCoordinate(y);
            int sz = ChunkUtil.chunkCoordinate(z);
            long sectionKey = ((long)(sx & 0xFFFFFF) << 40) | ((long)(sy & 0xFFFF) << 24) | (sz & 0xFFFFFF);

            Ref<ChunkStore> sectionRef;
            if (!sectionRefCache.containsKey(sectionKey)) {
                sectionRef = world.getChunkStore().getChunkSectionReference(sx, sy, sz);
                sectionRefCache.put(sectionKey, sectionRef);
            } else {
                sectionRef = sectionRefCache.get(sectionKey);
            }
            if (sectionRef == null) return false;

            float savedProgress   = farmingBlock.getGrowthProgress();
            int   savedGeneration = farmingBlock.getGeneration();

            // Resolve game time so the engine's natural tick timer is kept correct
            // after our forced advancement (mirrors ChangeFarmingStageInteraction).
            java.time.Instant gameTime = null;
            try {
                WorldTimeResource wtr = (WorldTimeResource) entityAccessor.getResource(
                        WorldTimeResource.getResourceType());
                if (wtr != null) gameTime = wtr.getGameTime();
            } catch (Exception ignored) {}

            // Prefab→Prefab (tree stage upgrade): pass the current stage so apply()
            // uses the compare() path, which only checks *newly added* blocks for
            // obstruction and replaces old-stage tree blocks rather than treating
            // them as blockers.  For the first prefab application (sapling→stage-1)
            // pass null so a fresh placement is done with no prior prefab to diff.
            FarmingStageData prevStage;
            if (stages[targetStage] instanceof PrefabFarmingStageData
                    && currentStage > 0
                    && stages[currentStage] instanceof PrefabFarmingStageData) {
                prevStage = stages[currentStage];
            } else if (stages[targetStage] instanceof PrefabFarmingStageData) {
                prevStage = null;
            } else {
                prevStage = currentStage > 0 ? stages[currentStage] : null;
            }

            System.out.println("[EZGrow] " + bid + " at " + x + "," + y + "," + z
                    + " stage " + currentStage + "->" + targetStage
                    + " stageType=" + stages[targetStage].getClass().getSimpleName()
                    + " prevStage=" + (prevStage == null ? "null" : prevStage.getClass().getSimpleName()));
            if (Math.random() >= com.Ev0sMods.Ev0sEZGrow.EzGrowConfig.growthChance) return false;

            boolean advanced = false;
            try {
                farmingBlock.setGrowthProgress((float) targetStage);
                farmingBlock.setGeneration(savedGeneration + 1);
                farmingBlock.setExecutions(0);
                if (gameTime != null) farmingBlock.setLastTickGameTime(gameTime);
                stages[targetStage].apply(chunkStore, sectionRef, blockRef, x, y, z, prevStage);
                advanced = true;
                System.out.println("[EZGrow] apply OK -> progress now " + farmingBlock.getGrowthProgress());
            } catch (Exception e) {
                farmingBlock.setGrowthProgress(savedProgress);
                farmingBlock.setGeneration(savedGeneration);
                System.out.println("[EZGrow] apply FAILED: " + e);
            }
            if (advanced) {
                try { chunk.setTicking(x, y, z, true); } catch (Exception ignored) {}
                spawnSparkle(x, y, z, entityAccessor);
            }
            return advanced;
        } catch (Exception e) {
            System.out.println("[EZGrow] outer ex at " + x + "," + y + "," + z + ": " + e);
            return false;
        }
    }

    private static void spawnSparkle(int x, int y, int z, Store<EntityStore> accessor) {
        try {
            ParticleUtil.spawnParticleEffect("Water_Can_Splash",
                    x + 0.5, y + 0.5, z + 0.5,
                    (java.util.List<Ref<EntityStore>>) null,
                    accessor);
        } catch (Exception ignored) {}
    }
}
