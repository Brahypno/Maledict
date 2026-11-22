package org.brahypno.maledict.common.enchantment;

import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import net.minecraft.world.level.Level;

/**
 * 复视为普通 Malum 镰刀投出的幻影。
 *
 * <p>它照常结算存在自己身上的伤害，但绝不碰掷出者的物品栏：返程一旦到家门口就在 Malum 的接取
 * 分支之前 {@code discard()}，所以既不会复原武器槽、也不会写进存档。原投掷是唯一允许调用
 * {@code ReboundHandler#pickupScythe} 的实体。
 *
 * <p>1.8.2 的 {@code ScytheBoomerangEntity} 两个构造器都把实体类型写死成
 * {@code MalumEntities.SCYTHE_BOOMERANG}（javap：{@code <init>(Level)} 与 {@code <init>(Level,DDD)}
 * 都直接读那个注册项），所以幻影沿用 Malum 的实体类型；客户端靠同步过来的物品堆叠上的
 * {@link DiplopiaThrows#ECHO_TAG} 认出它，渲染器见 {@code DiplopiaScytheBoomerangRenderer}。
 */
public final class DiplopiaScytheBoomerangEntity extends ScytheBoomerangEntity {
    public DiplopiaScytheBoomerangEntity(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && DiplopiaThrows.reachedOwnerOnReturn(this)){
            discard();
            return;
        }
        super.tick();
    }

    /**
     * 幻影只活在这一趟投掷里：万一在半空中被路过卸载的区块存盘，读回来会变成一把普通回旋镰刀，
     * 返程时把一把幻影镰刀塞回玩家物品栏。
     */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
