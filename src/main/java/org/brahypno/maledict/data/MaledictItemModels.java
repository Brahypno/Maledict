package org.brahypno.maledict.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;

public final class MaledictItemModels extends ItemModelProvider {
    public MaledictItemModels(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Maledict.MODID, existingFiles);
    }

    @Override
    protected void registerModels() {
        getBuilder("incursus_blade").parent(new ModelFile.UncheckedModelFile(
                new ResourceLocation("malum", "item/soul_stained_steel_scythe")));
    }
}
