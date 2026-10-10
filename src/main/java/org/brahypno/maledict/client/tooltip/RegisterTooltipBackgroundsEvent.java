package org.brahypno.maledict.client.tooltip;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fired once on each mod's MOD bus during queued client setup, on the client main thread.
 * Each registration binds a match rule to a complete artwork / geometry / mode combination.
 * Subscribe from a Dist.CLIENT event subscriber. No default combinations are installed.
 */
public final class RegisterTooltipBackgroundsEvent extends Event implements IModBusEvent {
    private final List<Registration> registrations = new ArrayList<>();

    // Forge's EventListenerHelper uses Class#getConstructor() when registering subscribers.
    public RegisterTooltipBackgroundsEvent() {}

    public void register(Item item, TooltipBackground combination) {
        Objects.requireNonNull(item);
        register(stack -> stack.is(item), combination);
    }

    public void register(Predicate<ItemStack> matches, TooltipBackground combination) {
        Objects.requireNonNull(combination);
        register(matches, stack -> combination);
    }

    public void register(Item item, Function<ItemStack, TooltipBackground> selectCombination) {
        Objects.requireNonNull(item);
        register(stack -> stack.is(item), selectCombination);
    }

    /**
     * Both callbacks run on every hover, so tags and NBT may determine the combination dynamically.
     * Later registrations take precedence. A null selection falls through to earlier registrations.
     */
    public void register(Predicate<ItemStack> matches, Function<ItemStack, TooltipBackground> selectCombination) {
        registrations.add(new Registration(Objects.requireNonNull(matches), Objects.requireNonNull(selectCombination)));
    }

    List<Registration> registrations() {
        return List.copyOf(registrations);
    }

    record Registration(Predicate<ItemStack> matches, Function<ItemStack, TooltipBackground> selectCombination) {}
}
