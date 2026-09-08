package org.brahypno.maledict.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new MaledictItemModels(output, existingFiles));
        generator.addProvider(event.includeClient(), new MaledictBlockStates(output, existingFiles));
        generator.addProvider(event.includeClient(), new MaledictLanguage(output, "en_us"));
        generator.addProvider(event.includeClient(), new MaledictLanguage(output, "zh_cn"));

        generator.addProvider(event.includeServer(), new MaledictRecipes(output));
        generator.addProvider(event.includeServer(), new LootTableProvider(
                output,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(
                        MaledictBlockLoot::new, LootContextParamSets.BLOCK))));
        MaledictBlockTags blockTags = new MaledictBlockTags(output, event.getLookupProvider(), existingFiles);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new MaledictItemTags(
                output, event.getLookupProvider(), blockTags.contentsGetter(), existingFiles));
    }

    private MaledictDataGenerators() {
    }
}
