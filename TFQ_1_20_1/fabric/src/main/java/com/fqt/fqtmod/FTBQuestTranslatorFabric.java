package com.fqt.fqtmod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;

public class FTBQuestTranslatorFabric implements ClientModInitializer {
    public static final String MODID = "ftbquesttransl";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        // Load config
        FabricConfig.load();
        SecretsConfig.load();
        LOGGER.info("FTB Quest Translator (Fabric) loaded!");
    }
}
