package org.brahypno.maledict.common.effect;

/**
 * 「熟成之赐」把经验抬高的那份算术。
 *
 * <p>原版发经验的入口是 {@code Player#giveExperiencePoints}，Forge 在它上面开了
 * {@code PlayerXpEvent.XpChange}，本效果每级把这一次的量乘以 {@code 1 + 0.25}——
 * 等级 I 多四分之一，等级 II 多一半，依此类推。
 *
 * <h2>零头为什么要掷骰</h2>
 * 经验是一次一点地发的：{@code 1 × 0.25} 只有 0.25，向下取整就永远是 0，等级 I 等于没有；
 * 向上取整又变成 1 点给 1 点，即 +100%，直接超模。所以这里把整数部分照发、小数部分按概率进位：
 * 掷骰小于零头就多给一点。单次看是随机的，长期看正好是四分之一，一颗经验球也不会被吞掉。
 *
 * <p>随机数由调用方掷好传进来（{@code LivingEntity#getRandom}），这个类因此不依赖任何
 * Minecraft 类型，可以直接单测，与 {@link BlessedRegeneration} 同一分工。
 */
public final class RipeningBonus {

    /** 每级多给的比例，{@code 0.25} 即四分之一。等级乘数由 {@link #ripened} 结算，这里只写单级的值。 */
    public static final float RATIO_PER_LEVEL = 0.25F;

    /**
     * @param amount    原版这一次要发放的经验
     * @param amplifier 效果等级，0 即等级 I
     * @param roll      0 到 1 之间的随机数，决定小数部分是否进位
     * @return 加成之后应当发放的经验；经验不为正、等级非法时原样返回
     */
    public static int ripened(int amount, int amplifier, float roll) {
        if (amount <= 0 || amplifier < 0) {
            return amount;
        }

        float exact = amount * RATIO_PER_LEVEL * (amplifier + 1);
        int whole = (int) exact;
        return amount + whole + (roll < exact - whole ? 1 : 0);
    }

    private RipeningBonus() {
    }
}
