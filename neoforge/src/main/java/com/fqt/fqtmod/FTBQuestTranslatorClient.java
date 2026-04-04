package com.fqt.fqtmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = FTBQuestTranslator.MODID, dist = Dist.CLIENT)
public class FTBQuestTranslatorClient {
    public FTBQuestTranslatorClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        FTBQuestTranslator.LOGGER.info("FTB Quest Translator client initialized!");
    }
}
