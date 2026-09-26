package org.brahypno.maledict.common.vitals;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

/**
 * 延迟池挂在玩家身上，而不是挂在符文上：符文只是开关，一旦入队就必须还清，摘下符文、符文被没收
 * 都不影响结算。挂在 {@link Player} 上是因为只有玩家会佩戴符文，客户端那一份只当显示用。
 */
@AutoRegisterCapability
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class DelayedVitals implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<DelayedVitals> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "delayed_vitals");

    private static final String POOL_TAG = "Pool";

    private final DelayedVitalsPool pool = new DelayedVitalsPool();

    /** 缓存下来复用：{@code getCapability} 会被问得非常频繁。 */
    private final LazyOptional<DelayedVitals> optional = LazyOptional.of(() -> this);

    /** 本 tick 有变化、需要给客户端发一包。 */
    private boolean dirty;

    /**
     * 当前这批待扣除里最早那条伤害来源，用来在延迟伤害致死时给对死因。
     *
     * <p>不落盘：存档里躺着一条 {@code DamageSource} 既存不下也没意义，重启后那批伤害退回无主伤害。
     */
    @Nullable
    private DamageSource earliestSource;

    /** 符文只对玩家开放，非玩家实体不挂，省掉一堆没用的 capability。 */
    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(KEY, new DelayedVitals());
        }
    }

    @Nullable
    public static DelayedVitals get(Entity entity) {
        return entity.getCapability(CAPABILITY).orElse(null);
    }

    public DelayedVitalsPool pool() {
        return pool;
    }

    /** 入队一记待扣除；会先把攒着的待治疗抵掉，抵不完的才排队。 */
    public void queueDamage(float amount, DamageSource source) {
        if (amount <= 0.0F) {
            return;
        }
        if (pool.pendingDamage() <= 0.0F) {
            earliestSource = source;
        }
        if (pool.netDamage(amount) <= 0.0F) {
            // 全被攒着的治疗抵掉了，这批没有要还的账，也就没有死因可记。
            earliestSource = null;
        }
        dirty = true;
    }

    /** 入队一记待治疗；它会先把待扣除抵掉。 */
    public void queueHeal(float amount) {
        if (amount <= 0.0F) {
            return;
        }
        pool.netHeal(amount);
        if (pool.pendingDamage() <= 0.0F) {
            earliestSource = null;
        }
        dirty = true;
    }

    @Nullable
    public DamageSource earliestSource() {
        return earliestSource;
    }

    /** 这批扣完了，来源一并丢掉，免得抓着已经不存在的实体不放。 */
    public void forgetSource() {
        earliestSource = null;
    }

    /** 死亡清除：两个池子与来源一起清。 */
    public void clear() {
        pool.clear();
        earliestSource = null;
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    /** 同步/对齐用：直接覆盖两个池子。 */
    public void set(float damage, float heal) {
        pool.set(damage, heal);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag poolTag = new CompoundTag();
        poolTag.putFloat("Damage", pool.pendingDamage());
        poolTag.putFloat("Heal", pool.pendingHeal());
        tag.put(POOL_TAG, poolTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        CompoundTag poolTag = tag.getCompound(POOL_TAG);
        pool.set(poolTag.getFloat("Damage"), poolTag.getFloat("Heal"));
    }
}
