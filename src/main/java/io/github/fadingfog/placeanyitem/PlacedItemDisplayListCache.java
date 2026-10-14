package io.github.fadingfog.placeanyitem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class PlacedItemDisplayListCache {

    private static final long GLINT_TTL_MS = 33L;
    private static final long ANIMATED_TTL_MS = 50L;
    private static final long PRUNE_INTERVAL_MS = 1000L;

    private final Map<TilePlacedItem, CacheEntry> displayListCache = new HashMap<>();
    private int atlasVersion;
    private long nextPruneMs;

    public PlacedItemDisplayListCache() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public CacheEntry getOrCompile(TilePlacedItem tile, ItemStack stack, int meta, Runnable renderBody) {
        CacheEntry entry = displayListCache.get(tile);
        if (entry == null || !entry.isValid(stack, meta, tile.rotation, atlasVersion) || entry.needsRefresh()) {
            dispose(tile);
            entry = compile(tile, stack, meta, renderBody);
        }
        return entry;
    }

    private CacheEntry compile(TilePlacedItem tile, ItemStack stack, int meta, Runnable renderBody) {
        int listId = GL11.glGenLists(1);
        // Bind the item's atlas for real: RenderItem saves the filter of the texture bound in GL, not in the list
        TextureManager textures = Minecraft.getMinecraft()
            .getTextureManager();
        textures.bindTexture(textures.getResourceLocation(stack.getItemSpriteNumber()));
        GL11.glNewList(listId, GL11.GL_COMPILE);
        try {
            renderBody.run();
            GL11.glEndList();
        } catch (RuntimeException | Error t) {
            GL11.glEndList();
            GL11.glDeleteLists(listId, 1);
            RenderItem.renderInFrame = false;
            throw t;
        }
        CacheEntry entry = new CacheEntry(
            listId,
            stack,
            stack.stackSize,
            meta,
            tile.rotation,
            getTTL(stack),
            atlasVersion);
        displayListCache.put(tile, entry);
        return entry;
    }

    private long getTTL(ItemStack stack) {
        if (stack.hasEffect()) return GLINT_TTL_MS; // Glint scroll (~30 fps)
        if (stack.getItem()
            .getIconIndex(stack) instanceof ITickable) return ANIMATED_TTL_MS; // 20 Hz sprite ticks
        return Long.MAX_VALUE; // static (event-invalidated)
    }

    public void dispose(TilePlacedItem tile) {
        CacheEntry entry = displayListCache.remove(tile);
        if (entry != null) GL11.glDeleteLists(entry.listId, 1);
    }

    private void pruneInvalidEntries() {
        if (displayListCache.isEmpty()) return;
        long now = Minecraft.getSystemTime();
        if (now < nextPruneMs) return;
        nextPruneMs = now + PRUNE_INTERVAL_MS;

        Iterator<Map.Entry<TilePlacedItem, CacheEntry>> it = displayListCache.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<TilePlacedItem, CacheEntry> entry = it.next();
            TilePlacedItem tile = entry.getKey();
            if (tile.isInvalid() || tile.getWorldObj() == null || tile.getStack() == null) {
                GL11.glDeleteLists(entry.getValue().listId, 1);
                it.remove();
            }
        }
    }

    private void clearAll() {
        for (CacheEntry entry : displayListCache.values()) {
            GL11.glDeleteLists(entry.listId, 1);
        }
        displayListCache.clear();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        pruneInvalidEntries();
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Post event) {
        atlasVersion++;
        clearAll();
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.world.isRemote) return;
        Chunk chunk = event.getChunk();
        Iterator<Map.Entry<TilePlacedItem, CacheEntry>> it = displayListCache.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<TilePlacedItem, CacheEntry> e = it.next();
            TilePlacedItem tile = e.getKey();
            if ((tile.xCoord >> 4) == chunk.xPosition && (tile.zCoord >> 4) == chunk.zPosition) {
                GL11.glDeleteLists(e.getValue().listId, 1);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (!event.world.isRemote) return;
        clearAll();
    }

    public static class CacheEntry {

        public final int listId;
        public final ItemStack stack;
        public final int stackSize;
        public final int meta;
        public final float rotation;
        public final long ttlMs;
        public final int atlasVersion;
        private long lastCompileMs;

        private CacheEntry(int listId, ItemStack stack, int stackSize, int meta, float rotation, long ttlMs,
            int atlasVersion) {
            this.listId = listId;
            this.stack = stack;
            this.stackSize = stackSize;
            this.meta = meta;
            this.rotation = rotation;
            this.ttlMs = ttlMs;
            this.atlasVersion = atlasVersion;
            this.lastCompileMs = System.currentTimeMillis();
        }

        private boolean isValid(ItemStack stack, int meta, float rotation, int atlasVersion) {
            // checking the pointer of the stack is enough
            return this.stack == stack && this.stackSize == stack.stackSize
                && this.meta == meta
                && this.rotation == rotation
                && this.atlasVersion == atlasVersion;
        }

        private boolean needsRefresh() {
            return ttlMs != Long.MAX_VALUE && System.currentTimeMillis() - lastCompileMs >= ttlMs;
        }
    }
}
