package org.brahypno.maledict.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new MaledictItemModels(output, existingFiles));
        generator.addProvider(event.includeClient(), new MaledictLanguage(output, "en_us"));
        generator.addProvider(event.includeClient(), new MaledictLanguage(output, "zh_cn"));

        generator.addProvider(event.includeServer(), new MaledictRecipes(output));
        MaledictBlockTags blockTags = new MaledictBlockTags(output, event.getLookupProvider(), existingFiles);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new MaledictItemTags(
                output, event.getLookupProvider(), blockTags.contentsGetter(), existingFiles));
    }

    private MaledictDataGenerators() {
    }
}
