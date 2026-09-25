package org.brahypno.maledict.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Blender 导出的刚性网格，挂在原版 ModelPart 关节层级上；动画、持械变换与判定锚点依旧走 {@link VicissitudeRig}。
 */
public final class FirstVicissitudeBossModel
        extends HierarchicalModel<FirstVicissitudeBossEntity> {
    private final ModelPart root;
    private final VicissitudeBlenderMesh mesh = new VicissitudeBlenderMesh();
    private final Map<VicissitudeRigData.Joint, ModelPart> parts =
            new EnumMap<>(VicissitudeRigData.Joint.class);
    private final VicissitudeRig.Pose pose = VicissitudeRig.newPose();
    private final VicissitudeRig.Pose releasePose = VicissitudeRig.newPose();
    private float sheddingTicks;
    private float renderAge;
    private float renderWingFold;

    float sheddingTicks() { return sheddingTicks; }

    void poseStackToRelease(VicissitudeRigData.Joint joint, PoseStack stack, float delay, float elapsed) {
        VicissitudeRig.compute(releasePose, false, delay / FirstVicissitudeBossEntity.TRANSITION_TICKS,
                VicissitudeRig.Action.NONE, 0, false, 0, renderAge - elapsed, renderWingFold, -1);
        List<VicissitudeRigData.Joint> chain = new ArrayList<>();
        for (var current = joint; current != null; current = current.parent()) chain.add(current);
        for (int i = chain.size()-1; i >= 0; i--) {
            var j = chain.get(i);
            stack.translate((j.localX()+releasePose.offX(j))/16F,
                    (j.localY()+releasePose.offY(j))/16F, (j.localZ()+releasePose.offZ(j))/16F);
            stack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(releasePose.rotZ(j)));
            stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(releasePose.rotY(j)));
            stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(releasePose.rotX(j)));
        }
    }

    public FirstVicissitudeBossModel(ModelPart root) {
        this.root = root;
        ModelPart skeletonRoot = root.getChild("root");
        for (VicissitudeRigData.Joint joint : VicissitudeRigData.Joint.values()) {
            ModelPart parent = joint.parent() == null ? skeletonRoot : parts.get(joint.parent());
            parts.put(joint, parent.getChild(joint.partName()));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition skeletonRoot = meshRoot.addOrReplaceChild(
                "root", CubeListBuilder.create(), PartPose.ZERO);
        Map<VicissitudeRigData.Joint, PartDefinition> definitions =
                new EnumMap<>(VicissitudeRigData.Joint.class);
        for (VicissitudeRigData.Joint joint : VicissitudeRigData.Joint.values()) {
            PartDefinition parent = joint.parent() == null
                                    ? skeletonRoot : definitions.get(joint.parent());
            PartDefinition definition = parent.addOrReplaceChild(
                    joint.partName(), CubeListBuilder.create(),
                    PartPose.offset(joint.localX(), joint.localY(), joint.localZ()));
            definitions.put(joint, definition);
        }
        return LayerDefinition.create(mesh, VicissitudeRigData.TEXTURE_SIZE,
                VicissitudeRigData.TEXTURE_SIZE);
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        mesh.render(this, stack, buffer, light, overlay, red, green, blue, alpha);
    }

    public ModelPart part(VicissitudeRigData.Joint joint) {
        return parts.get(joint);
    }

    /**
     * 沿整条祖先链把 pose stack 挪到某个关节：{@link ModelPart#translateAndRotate} 只应用该部件自身的局部变换，
     * 直接用在右手锚点这种深层关节上会让物品停在模型原点，所以这里与网格渲染一样从根开始。
     */
    public void poseStackTo(VicissitudeRigData.Joint joint, PoseStack poseStack) {
        List<ModelPart> chain = new ArrayList<>(8);
        for (VicissitudeRigData.Joint current = joint; current != null; current = current.parent()) {
            ModelPart part = parts.get(current);
            if (part != null) {
                chain.add(part);
            }
        }
        for (int index = chain.size() - 1; index >= 0; index--) {
            chain.get(index).translateAndRotate(poseStack);
        }
    }

    public VicissitudeRig.Pose pose() {
        return pose;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FirstVicissitudeBossEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        // 与特效锚点采同一个时钟和姿势，不随渲染频率变化。
        float partialTick = net.minecraft.util.Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        renderAge = entity.level().getGameTime() + partialTick;
        renderWingFold = entity.getWingFold();
        float blend = entity.getPhaseTwoBlend();
        sheddingTicks = blend * FirstVicissitudeBossEntity.TRANSITION_TICKS;
        if (blend > 0 && blend < 1) sheddingTicks += partialTick;
        pose.copyFrom(entity.poseForRender(partialTick));
        applyPose(pose);
    }

    private void applyPose(VicissitudeRig.Pose applied) {
        for (VicissitudeRigData.Joint joint : VicissitudeRigData.Joint.values()) {
            ModelPart part = parts.get(joint);
            if (part == null) {
                continue;
            }
            part.xRot = (float) Math.toRadians(applied.rotX(joint));
            part.yRot = (float) Math.toRadians(applied.rotY(joint));
            part.zRot = (float) Math.toRadians(applied.rotZ(joint));
            part.x = joint.localX() + applied.offX(joint);
            part.y = joint.localY() + applied.offY(joint);
            part.z = joint.localZ() + applied.offZ(joint);
        }
    }
}
