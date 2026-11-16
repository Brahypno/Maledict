package org.brahypno.maledict.common.entity.vicissitude;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity.BossSpawnPhase;
import org.brahypno.maledict.common.item.StatedSpawnEggItem;

/**
 * 无常的刷怪蛋，一个阶段一个：落地的同时就把这一只安排到那个阶段。
 * {@code setSpawnPhase} 必须赶在 {@code onAddedToWorld} <b>之前</b>（血量池在那里按当时的难度捕获），
 * 所以状态写入走 {@link StatedSpawnEggItem} 的 {@code onSpawn} 回调：{@code EntityType#create} 之后、
 * {@code addFreshEntityWithPassengers} 之前。
 */
public class VicissitudeSpawnEggItem extends StatedSpawnEggItem {
    public VicissitudeSpawnEggItem(
            RegistryObject<? extends EntityType<? extends FirstVicissitudeBossEntity>> type,
            BossDifficulty difficulty, BossSpawnPhase spawnPhase,
            int backgroundColor, int highlightColor, Item.Properties properties) {
        super(type, backgroundColor, highlightColor, properties, true, entity -> {
            if (entity instanceof FirstVicissitudeBossEntity boss) {
                boss.setSpawnPhase(difficulty, spawnPhase);
            }
        });
    }
}
