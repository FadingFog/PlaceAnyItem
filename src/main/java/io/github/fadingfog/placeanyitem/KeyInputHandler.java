package io.github.fadingfog.placeanyitem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class KeyInputHandler {

    private final KeyBinding placeItem;

    public KeyInputHandler() {
        placeItem = new KeyBinding("key.placeanyitem.placeItem", Keyboard.KEY_P, "PlaceAnyItem");
        ClientRegistry.registerKeyBinding(placeItem);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (this.placeItem.isPressed()) handlePlaceItemKey();
    }

    // The key can be bound to a mouse button.
    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (this.placeItem.isPressed()) handlePlaceItemKey();
    }

    private void handlePlaceItemKey() {
        if (!Config.allowPlacing) return;
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        MovingObjectPosition mop = raytraceFromEntity(player.worldObj, player, 4.5D);
        if (mop != null) {
            PlaceAnyItem.network
                .sendToServer(new PlacedItemPacket((byte) mop.sideHit, mop.blockX, mop.blockY, mop.blockZ));
        }
    }

    private static MovingObjectPosition raytraceFromEntity(World world, Entity player, double range) {
        float f = 1.0F;
        float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double) f;
        double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double) f;
        double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double) f;
        Vec3 vec3 = Vec3.createVectorHelper(d0, d1, d2);
        float f3 = MathHelper.cos(-f2 * 0.017453292F - (float) Math.PI);
        float f4 = MathHelper.sin(-f2 * 0.017453292F - (float) Math.PI);
        float f5 = -MathHelper.cos(-f1 * 0.017453292F);
        float f6 = MathHelper.sin(-f1 * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        Vec3 vec31 = vec3.addVector((double) f7 * range, (double) f6 * range, (double) f8 * range);
        return world.rayTraceBlocks(vec3, vec31);
    }
}
