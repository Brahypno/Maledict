package org.brahypno.maledict.client.tooltip;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.IModBusEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

class RegisterTooltipBackgroundsEventTest {
    @Test
    void forgeCanRegisterAnAnnotatedSubscriberAndDispatchTheRegistrationEvent() {
        var bus = BusBuilder.builder().markerType(IModBusEvent.class).build();
        var subscriber = new Subscriber();
        // This follows the EventListenerHelper reflection path that failed during mod construction.
        assertDoesNotThrow(() -> bus.register(subscriber));
        var event = new RegisterTooltipBackgroundsEvent();
        bus.post(event);
        assertSame(event, subscriber.received);
    }

    public static final class Subscriber {
        private RegisterTooltipBackgroundsEvent received;

        @SubscribeEvent
        public void onRegister(RegisterTooltipBackgroundsEvent event) {
            received = event;
        }
    }
}
