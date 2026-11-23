package org.brahypno.maledict.data;

import org.brahypno.maledict.data.worldgen.MaledictBiomeModifiers;
import org.brahypno.maledict.data.tags.MaledictBiomeTags;
import org.brahypno.maledict.data.loot.MaledictBlockLoot;
import org.brahypno.maledict.data.loot.MaledictRealmLoot;
import org.brahypno.maledict.data.client.MaledictBlockStates;
import org.brahypno.maledict.data.tags.MaledictBlockTags;
import org.brahypno.maledict.data.tags.MaledictEntityTypeTags;
import org.brahypno.maledict.data.tags.MaledictDamageTypeTags;
import org.brahypno.maledict.data.loot.MaledictEntityLoot;
import org.brahypno.maledict.data.client.MaledictItemModels;
import org.brahypno.maledict.data.tags.MaledictItemTags;
import org.brahypno.maledict.data.client.MaledictLanguage;
import org.brahypno.maledict.data.recipe.MaledictRecipes;
import org.brahypno.maledict.data.worldgen.MaledictWorldgen;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Maledict.MODID)
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

        generator.addProvider(event.includeServer(), new MaledictRecipes(output, event.getLookupProvider()));
        MaledictWorldgen worldgen = new MaledictWorldgen(output, event.getLookupProvider());
        generator.addProvider(event.includeServer(), worldgen);
        generator.addProvider(event.includeServer(), new MaledictBiomeTags(output, worldgen.getRegistryProvider(), existingFiles));
        generator.addProvider(event.includeServer(), new MaledictBiomeModifiers(output));
        generator.addProvider(event.includeServer(), new MaledictSpiritData(output));
        // 伤害类型与它们的标签：JSON 只从这两个 provider 出，别手写进 src/main/resources。
        // 标签必须读 entries provider 打补丁后的 registry lookup，否则 tags 阶段还看不到自家的伤害类型。
        MaledictDamageTypeEntries damageTypes = new MaledictDamageTypeEntries(output, event.getLookupProvider());
        generator.addProvider(event.includeServer(), damageTypes);
        generator.addProvider(event.includeServer(), new MaledictDamageTypeTags(
                output, damageTypes.getRegistryProvider(), existingFiles));
        // 三份 35×20×35 的 .nbt 只由这里写出：几何源码在 MaledictReliquaryTemplates，生成物在 data/maledict/structures。
        generator.addProvider(event.includeServer(), new MaledictReliquaryTemplates(output));
        generator.addProvider(event.includeServer(), new LootTableProvider(
                output,
                Set.of(),
                List.of(
                        new LootTableProvider.SubProviderEntry(
                                MaledictBlockLoot::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(
                                MaledictEntityLoot::new, LootContextParamSets.ENTITY),
                        new LootTableProvider.SubProviderEntry(
                                MaledictRealmLoot::new, LootContextParamSets.CHEST),
                        new LootTableProvider.SubProviderEntry(
                                registries -> consumer -> consumer.accept(
                                        net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "rewards/wisdom_sacrifice")),
                                        LootTable.lootTable().withPool(LootPool.lootPool()
                                                .setRolls(ConstantValue.exactly(1))
                                                .add(LootItem.lootTableItem(MaledictItems.BLEEDING_EYE.get())))),
                                LootContextParamSets.ADVANCEMENT_REWARD)), event.getLookupProvider()));
        MaledictBlockTags blockTags = new MaledictBlockTags(output, event.getLookupProvider(), existingFiles);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new MaledictItemTags(
                output, event.getLookupProvider(), blockTags.contentsGetter(), existingFiles));
        generator.addProvider(event.includeServer(), new MaledictEntityTypeTags(
                output, event.getLookupProvider(), existingFiles));
    }

    private MaledictDataGenerators() {
    }
}
