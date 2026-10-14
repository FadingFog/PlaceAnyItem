package io.github.fadingfog.placeanyitem;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

// NEI finds this class by its name; without NEI installed it is never loaded.
public class NEIPlaceAnyItemConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        if (PlaceAnyItem.placedItem != null) API.hideItem(new ItemStack(PlaceAnyItem.placedItem));
    }

    @Override
    public String getName() {
        return "PlaceAnyItem";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
