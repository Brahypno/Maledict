package org.brahypno.maledict.client.tooltip;

import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.IModBusEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

class RegisterTooltipBackgroundsEventTest {
    @Test
    void registrationEventCanReachAnAnnotatedModBusSubscriber() {
        var bus = BusBuilder.builder().markerType(IModBusEvent.class).build();
        var subscriber = new Subscriber();
        // Guards this mod's event visibility and bus marker, which previously broke subscriber registration.
        assertDoesNotThrow(() -> bus.register(subscriber));
        var event = new RegisterTooltipBackgroundsEvent();
        bus.post(event);
        assertSame(event, subscriber.received);
    }

    public static final class Subscriber {
        private RegisterTooltipBackgroundsEvent received;

        @SubscribeEvent
        public void onRegister(RegisterTooltipBackgroundsEvent event) { received = event; }
    }
}
