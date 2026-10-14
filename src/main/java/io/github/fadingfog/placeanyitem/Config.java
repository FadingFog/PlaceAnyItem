package io.github.fadingfog.placeanyitem;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean allowPlacing = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        allowPlacing = configuration.getBoolean(
            "allowPlacing",
            Configuration.CATEGORY_GENERAL,
            allowPlacing,
            "Allow placing held items in the world. Items placed earlier can still be broken when this is false.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
