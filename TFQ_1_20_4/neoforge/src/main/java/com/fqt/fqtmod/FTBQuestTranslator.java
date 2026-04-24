package com.fqt.fqtmod;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(FTBQuestTranslator.MODID)
public class FTBQuestTranslator {
    public static final String MODID = "ftbquesttransl";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTBQuestTranslator(IEventBus modEventBus) {
        net.neoforged.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        LOGGER.info("FTB Quest Translator loaded!");
    }
}
