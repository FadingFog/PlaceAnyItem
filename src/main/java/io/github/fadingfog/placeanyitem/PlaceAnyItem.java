package io.github.fadingfog.placeanyitem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = PlaceAnyItem.MODID, version = Tags.VERSION, name = "PlaceAnyItem", acceptedMinecraftVersions = "[1.7.10]")
public class PlaceAnyItem {

    public static final String MODID = "placeanyitem";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "io.github.fadingfog.placeanyitem.ClientProxy",
        serverSide = "io.github.fadingfog.placeanyitem.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper network;
    public static PlacedItem placedItem;

    private static boolean disabled;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        disabled = Loader.isModLoaded("DraconicEvolution");
        if (disabled) {
            LOG.info("Draconic Evolution is installed and already places items, PlaceAnyItem stays inactive");
            return;
        }
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!disabled) proxy.init(event);
    }
}
