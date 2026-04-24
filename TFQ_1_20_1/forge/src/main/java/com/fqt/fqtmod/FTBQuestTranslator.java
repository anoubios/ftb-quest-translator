package com.fqt.fqtmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FTBQuestTranslator.MODID)
public class FTBQuestTranslator {
    public static final String MODID = "ftbquesttransl";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTBQuestTranslator() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOGGER.info("FTB Quest Translator loaded!");
    }
}
