package io.github.fadingfog.placeanyitem;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        // Registered even when placing is disabled in the config, so already placed items stay in the world.
        PlaceAnyItem.placedItem = new PlacedItem();
        GameRegistry.registerBlock(PlaceAnyItem.placedItem, "placedItem");
        GameRegistry.registerTileEntity(TilePlacedItem.class, PlaceAnyItem.MODID + ":TilePlacedItem");

        PlaceAnyItem.network = NetworkRegistry.INSTANCE.newSimpleChannel(PlaceAnyItem.MODID);
        PlaceAnyItem.network.registerMessage(PlacedItemPacket.Handler.class, PlacedItemPacket.class, 0, Side.SERVER);
    }

    public void init(FMLInitializationEvent event) {}
}
