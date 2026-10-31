package org.brahypno.maledict.client.realm;

import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.brahypno.maledict.common.realm.wither.WeepingWitherEntity;

/** The vanilla cubes and UVs, with a slightly broader, heavier resting silhouette. */
public final class WeepingWitherModel extends WitherBossModel<WitherBoss> {
    public WeepingWitherModel(ModelPart root) {
        super(root);
        root.getChild("shoulders").xScale = 1.12F;
        root.getChild("ribcage").xScale = 1.16F;
        root.getChild("ribcage").zScale = 1.30F;
        root.getChild("tail").zScale = 1.15F;
        root.getChild("right_head").x -= 1.0F;
        root.getChild("right_head").y += 1.3F;
        root.getChild("right_head").xScale = 0.96F;
        root.getChild("right_head").yScale = 1.04F;
        root.getChild("right_head").zScale = 0.96F;
        root.getChild("left_head").x += 1.0F;
        root.getChild("left_head").y += 1.3F;
        root.getChild("left_head").xScale = 0.90F;
        root.getChild("left_head").yScale = 0.97F;
        root.getChild("left_head").zScale = 0.93F;
    }

    @Override
    public void setupAnim(WitherBoss entity, float limbSwing, float limbAmount, float age,
                          float yaw, float pitch) {
        super.setupAnim(entity, limbSwing, limbAmount, age, yaw, pitch);
        boolean reforming = entity instanceof WeepingWitherEntity wither && wither.isReforming();
        root().getChild("center_head").xRot += reforming ? 0.28F : 0.15F;
        // prepareMobModel supplies the side heads' independent aiming before this method.
        root().getChild("right_head").xRot += reforming ? 0.30F : 0.11F;
        root().getChild("left_head").xRot += reforming ? 0.30F : 0.11F;
        root().getChild("right_head").zRot = -0.09F;
        root().getChild("left_head").zRot = 0.09F;
    }
}
