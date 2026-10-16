package io.github.fadingfog.placeanyitem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PlacedItemPacket implements IMessage {

    byte side = 0;
    int blockX = 0;
    int blockY = 0;
    int blockZ = 0;

    public PlacedItemPacket() {}

    public PlacedItemPacket(byte side, int x, int y, int z) {
        this.side = side;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
    }

    @Override
    public void toBytes(ByteBuf bytes) {
        bytes.writeByte(side);
        bytes.writeInt(blockX);
        bytes.writeInt(blockY);
        bytes.writeInt(blockZ);
    }

    @Override
    public void fromBytes(ByteBuf bytes) {
        this.side = bytes.readByte();
        this.blockX = bytes.readInt();
        this.blockY = bytes.readInt();
        this.blockZ = bytes.readInt();
    }

    public static class Handler implements IMessageHandler<PlacedItemPacket, IMessage> {

        // Packets arrive on the network thread; the world may only be changed on the server thread.
        private static final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();

        @Override
        public IMessage onMessage(PlacedItemPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            pending.add(() -> place(player, message));
            return null;
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            Runnable task;
            while ((task = pending.poll()) != null) task.run();
        }

        private static void place(EntityPlayer player, PlacedItemPacket message) {
            ForgeDirection dir = ForgeDirection.getOrientation(message.side);
            int x = message.blockX + dir.offsetX;
            int y = message.blockY + dir.offsetY;
            int z = message.blockZ + dir.offsetZ;
            World world = player.worldObj;

            if (!world.isAirBlock(x, y, z) || player.getHeldItem() == null || !Config.allowPlacing) return;

            // Refuse while a GUI is open: it may still edit the held item after it is placed and duplicate items.
            if (player.openContainer != player.inventoryContainer) return;

            BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(
                new BlockSnapshot(world, x, y, z, PlaceAnyItem.placedItem, 0),
                world.getBlock(message.blockX, message.blockY, message.blockZ),
                player);
            MinecraftForge.EVENT_BUS.post(event);

            if (event.isCanceled()) {
                return;
            }

            ItemStack stack = player.getHeldItem();

            world.setBlock(x, y, z, PlaceAnyItem.placedItem, message.side, 2);
            TilePlacedItem tile = (world.getTileEntity(x, y, z) != null
                && world.getTileEntity(x, y, z) instanceof TilePlacedItem)
                    ? (TilePlacedItem) world.getTileEntity(x, y, z)
                    : null;

            if (tile == null) {
                world.setBlockToAir(x, y, z);
                return;
            }

            tile.setStack(stack.copy());
            player.destroyCurrentEquippedItem();
        }
    }
}
