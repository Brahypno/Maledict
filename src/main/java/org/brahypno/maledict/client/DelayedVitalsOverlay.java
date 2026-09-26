package org.brahypno.maledict.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.vitals.DelayedVitals;
import org.brahypno.maledict.common.vitals.DelayedVitalsHearts;
import org.brahypno.maledict.common.vitals.DelayedVitalsPool;

/**
 * 延迟池在血条上的两个显示：待扣除与待治疗。
 *
 * <p><b>画在原版红心自己的格子上，不是额外一行。</b>延迟池不是额外血池，它就是这条血条上还没落地的
 * 那一段，所以坐标、行距、行数全部照抄原版，并且**绝不碰 {@code gui.leftHeight}** —— 那个值决定
 * 护甲行画在哪，动它会让护甲整体错位。
 *
 * <p>坐标来自 1.20.1 Forge：`ForgeGui#renderHealth`（{@code left = width/2 - 91}、
 * {@code top = height - 39}、{@code rows = ceil((maxHealth + absorb)/2/10)}、
 * {@code rowHeight = max(10 - (rows-2), 3)}）与 `Gui#renderHearts`（{@code x = left + (i%10)*8}、
 * {@code y = top - (i/10)*rowHeight}，下标大的行在上、先画）。低血抖动与再生抬升也一并复刻，
 * 否则抖动那几帧染色会和红心错开一格。
 *
 * <p>贴图是本模组自己的白色心形遮罩（满心 / 左半心），靠 {@code setColor} 上色；原版红心的颜色是
 * 烘焙进贴图的，直接染它只会越染越黑。
 */
public final class DelayedVitalsOverlay implements IGuiOverlay {

    /** 待扣除：邪恶精魂紫 {@code #792CEC}，画在「正在离开」的那几颗心上。 */
    public static final DelayedVitalsOverlay PENDING_DAMAGE =
            new DelayedVitalsOverlay(0x79 / 255.0F, 0x2C / 255.0F, 0xEC / 255.0F, 1.0F, true);

    /**
     * 待治疗：神圣精魂粉 {@code #EE2C88}，画在「正在回来」的那几颗心上。
     *
     * <p>两处都是不透明：贴图本身带明暗，{@code setColor} 又是乘法，半透明再叠一层底就会把颜色
     * 压暗一档（粉会看着发紫）。
     */
    public static final DelayedVitalsOverlay PENDING_HEAL =
            new DelayedVitalsOverlay(0xEE / 255.0F, 0x2C / 255.0F, 0x88 / 255.0F, 1.0F, false);

    private static final ResourceLocation HEART_MASK =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "textures/gui/delayed_vitals_hearts.png");

    private static final int HEART_SIZE = 9;

    /** 原版两颗心之间的横向间距，{@code Gui#HEART_SEPARATION}。 */
    private static final int HEART_SEPARATION = 8;

    private static final int MASK_SHEET_WIDTH = 29;
    private static final int MASK_SHEET_HEIGHT = 9;

    /** 遮罩图里满心、左半心、右半心的横向起点。 */
    private static final int MASK_U_FULL = 1;
    private static final int MASK_U_LEFT_HALF = 10;
    private static final int MASK_U_RIGHT_HALF = 19;

    /** 血量渲染开始时 {@code ForgeGui.leftHeight} 恒为 39，所以第一行红的 y 是 {@code height - 39}。 */
    private static final int HEALTH_BASE_HEIGHT = 39;

    /** 原版每帧用它重置抖动随机数（{@code ForgeGui#renderHealth}），照抄才能抖得一模一样。 */
    private static final long JITTER_SEED = 312871L;

    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    /** true = 看伤害池（从当前血量往回数），false = 看治疗池（从当前血量往上数）。 */
    private final boolean pendingDamage;

    /** 自己的抖动随机数；两个显示各持一份，同样的种子喂出同样的序列。 */
    private final RandomSource jitter = RandomSource.create();

    private DelayedVitalsOverlay(float red, float green, float blue, float alpha, boolean pendingDamage) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.pendingDamage = pendingDamage;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.options.hideGui || !gui.shouldDrawSurvivalElements()) {
            return;
        }
        if (!(minecraft.getCameraEntity() instanceof LocalPlayer player)) {
            return;
        }
        DelayedVitals vitals = DelayedVitals.get(player);
        if (vitals == null) {
            return;
        }

        DelayedVitalsPool pool = vitals.pool();
        float pending = pendingDamage ? pool.pendingDamage() : pool.pendingHeal();
        if (pending <= 0.0F) {
            return;
        }

        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        float healthMax = (float) maxHealth.getValue();
        int health = Mth.ceil(player.getHealth());
        int absorb = Mth.ceil(player.getAbsorptionAmount());

        int rows = Mth.ceil((healthMax + absorb) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (rows - 2), 3);
        int left = width / 2 - 91;
        int top = height - HEALTH_BASE_HEIGHT;

        int fullHearts = Mth.ceil(healthMax / 2.0D);
        int absorbHearts = Mth.ceil(absorb / 2.0D);
        int regen = player.hasEffect(MobEffects.REGENERATION)
                    ? gui.getGuiTicks() % Mth.ceil(healthMax + 5.0F)
                    : -1;

        DelayedVitalsHearts segment = pendingDamage
                                      ? DelayedVitalsHearts.pendingDamage(health, pending)
                                      : DelayedVitalsHearts.pendingHeal(health, pending, fullHearts * 2);
        if (segment.isEmpty()) {
            return;
        }

        boolean shaking = health + absorb <= 4;
        jitter.setSeed(gui.getGuiTicks() * JITTER_SEED);

        RenderSystem.enableBlend();
        graphics.setColor(red, green, blue, alpha);
        // 整条循环跑满，抖动随机数才和原版消耗在同一顺序上；不染的心只是不画。
        for (int index = fullHearts + absorbHearts - 1; index >= 0; --index) {
            int y = top - index / 10 * rowHeight;
            if (shaking) {
                y += jitter.nextInt(2);
            }
            if (index < fullHearts && index == regen) {
                y -= 2;
            }
            int mask = maskFor(segment.coverage(index));
            if (mask < 0) {
                continue;
            }
            int x = left + index % 10 * HEART_SEPARATION;
            graphics.blit(HEART_MASK, x, y, mask, 0, HEART_SIZE, HEART_SIZE,
                          MASK_SHEET_WIDTH, MASK_SHEET_HEIGHT);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** 覆盖情况对应遮罩图里的哪一格；没盖到返回 -1。 */
    private static int maskFor(DelayedVitalsHearts.Coverage coverage) {
        return switch (coverage) {
            case FULL -> MASK_U_FULL;
            case LEFT_HALF -> MASK_U_LEFT_HALF;
            case RIGHT_HALF -> MASK_U_RIGHT_HALF;
            case NONE -> -1;
        };
    }
}
