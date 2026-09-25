package org.brahypno.maledict.common.effect;

/**
 * 「熟成之赐」抬高经验的算术：每级把这一次的经验乘 {@code 1 + 0.25}。
 * 经验一次只发一点，取整会让等级 I 完全失效，故整数部分照发、小数部分按概率进位（由调用方掷骰传入）。
 */
public final class RipeningBonus {

    /** 每级多给的比例，{@code 0.25} 即四分之一。 */
    public static final float RATIO_PER_LEVEL = 0.25F;

    /**
     * @param amount    原版这一次要发放的经验
     * @param amplifier 效果等级，0 即等级 I
     * @param roll      0 到 1 之间的随机数，决定小数部分是否进位
     * @return 加成后应发放的经验；经验不为正或等级非法时原样返回
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
