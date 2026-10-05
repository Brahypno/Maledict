package org.brahypno.maledict.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.RavenEntity;

/**
 * Original geometry, following Alex's Mobs ModelCrow's root/body/head/beak/wing/leg/tail
 * hierarchy, mirrored shoulder pivots, reset pose and interpolated flight progress.
 * Vanilla model units: 16 pixels per block; feet at y=24; forward is negative Z.
 * Spread: x=-24..24, z=-16..16, y=8..24. Folded wings remain inside x=-8..8.
 */
public final class RavenModel extends HierarchicalModel<RavenEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "raven"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart tail;

    public RavenModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = body.getChild("head");
        leftWing = body.getChild("left_wing");
        rightWing = body.getChild("right_wing");
        leftLeg = body.getChild("left_leg");
        rightLeg = body.getChild("right_leg");
        tail = body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -2, -3, 8, 6, 10)
                .texOffs(0, 0).addBox(-3, -4, -7, 6, 8, 7)
                .texOffs(0, 0).addBox(-3, -2, 5, 6, 5, 6), PartPose.offset(0, 16, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-3, -4, -3, 6, 6, 6), PartPose.offset(0, -4, -7));
        head.addOrReplaceChild("beak", CubeListBuilder.create()
                .texOffs(112, 0).addBox(-1.5F, -1.5F, -8, 3, 2, 5)
                .texOffs(112, 10).addBox(-1, -1.0F, -9, 2, 1, 1), PartPose.ZERO);
        // Separate tiny eye cuboids give each side a red eye without mirrored face UV mistakes.
        head.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(96, 0).addBox(-3.1F, -2.5F, -1.8F, 0.2F, 1, 1)
                .texOffs(96, 0).addBox(2.9F, -2.5F, -1.8F, 0.2F, 1, 1), PartPose.ZERO);
        addWing(body, "left_wing", 1);
        addWing(body, "right_wing", -1);
        for (int side : new int[]{-1, 1}) {
            body.addOrReplaceChild(side == 1 ? "left_leg" : "right_leg", CubeListBuilder.create()
                    .texOffs(0, 40).addBox(-0.5F, 0, -0.5F, 1, 3, 1)
                    .texOffs(8, 40).addBox(-1.5F, 3, -2.5F, 3, 1, 4),
                    PartPose.offset(side * 2, 4, 2));
        }
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(),
                PartPose.offset(0, 1, 7));
        for (int i = 0; i < 5; i++) {
            // A stepped wedge, characteristic of a raven rather than a crow's straight tail.
            tail.addOrReplaceChild("feather_" + i, CubeListBuilder.create()
                    .texOffs(32, 40).addBox(-1, -0.5F, 0, 2, 1, 9 - Math.abs(i - 2)),
                    PartPose.offset((i - 2) * 1.5F, 0, 0));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addWing(PartDefinition body, String name, int side) {
        PartDefinition wing = body.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(48, 0).mirror(side < 0)
                .addBox(side > 0 ? 0 : -8, -1, -1, 8, 2, 7),
                PartPose.offset(side * 4, -1, -4));
        PartDefinition tip = wing.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(80, 16).mirror(side < 0)
                .addBox(side > 0 ? 0 : -12, -0.5F, 0, 12, 1, 4),
                PartPose.offset(side * 8, 0, 0));
        for (int i = 0; i < 7; i++) {
            // Overlapping individual primaries with stepped tips, never a rectangular wing plate.
            tip.addOrReplaceChild("primary_" + i, CubeListBuilder.create()
                    .texOffs(48, 16).mirror(side < 0)
                    .addBox(side > 0 ? 0 : -3, -0.35F, 0, 3, 0.7F, 9 - i * 0.6F),
                    PartPose.offset(side * (i * 1.5F), 0, 3));
        }
        for (int i = 0; i < 4; i++) {
            wing.addOrReplaceChild("secondary_" + i, CubeListBuilder.create()
                    .texOffs(48, 16).mirror(side < 0)
                    .addBox(side > 0 ? 0 : -2.5F, -0.4F, 0, 2.5F, 0.8F, 5 + i * 0.5F),
                    PartPose.offset(side * (i * 1.6F), 0, 5));
        }
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(RavenEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0, 1);
        float flight = Mth.lerp(partialTick, entity.previousFlightProgress, entity.flightProgress);
        float flap = Mth.sin(ageInTicks * 0.9F) * 0.32F * flight;
        foldWing(leftWing, 1, flight, flap);
        foldWing(rightWing, -1, flight, flap);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD
                + (entity.getPeckTicks() > 0 ? 0.45F : 0.0F);
        float hop = Mth.sin(limbSwing * 1.4F) * limbSwingAmount * (1 - flight);
        leftLeg.xRot = -flight * 0.85F + hop * 0.35F;
        rightLeg.xRot = leftLeg.xRot;
        tail.xRot = Mth.sin(ageInTicks * 0.12F) * 0.025F;
    }

    private static void foldWing(ModelPart wing, int side, float flight, float flap) {
        // Sweep backwards into the body's outline; feather stacks compress as a real folded wing.
        wing.x = side * Mth.lerp(flight, 7.7F, 4.0F);
        wing.yRot = -side * (1 - flight) * Mth.HALF_PI;
        wing.zRot = side * flap;
        wing.xScale = Mth.lerp(flight, 0.6F, 1.0F);
        wing.zScale = Mth.lerp(flight, 0.3F, 1.0F);
    }

    public void setupShoulderPose(float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        foldWing(leftWing, 1, 0.0F, 0.0F);
        foldWing(rightWing, -1, 0.0F, 0.0F);
        head.yRot = headYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
    }
}
