package io.openems.edge.battery.microcare.mk1;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.*;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.Arrays;
import java.util.stream.Stream;

public class Utils {
    public static Stream<? extends AbstractReadChannel<?>> initializeChannels(BatteryMK1 mk1) {
        return Stream.of( //
                Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
                    switch (channelId) {
                        case STATE:
                            return new StateCollectorChannel(mk1, channelId);
                    }
                    return null;
                }), Arrays.stream(Battery.ChannelId.values()).map(channelId -> {
                    switch (channelId) {
                        case SOC:
                        case SOH:
                        case BATTERY_TEMP:
                        case MAX_CAPACITY:
                        case CHARGE_MAX_CURRENT:
                        case CHARGE_MAX_VOLTAGE:
                        case DISCHARGE_MAX_CURRENT:
                        case DISCHARGE_MIN_VOLTAGE:
                        case CAPACITY_KWH:
                            return new IntegerReadChannel(mk1, channelId);
                        case READY_FOR_WORKING:
                            return new BooleanReadChannel(mk1, channelId);
                        default:
                            break;
                    }
                    return null;
                }), Arrays.stream(BatteryMK1.ChannelId.values()).map(channelId -> {
                    switch (channelId) {
                        case AVERAGE_CURRENT:
                        case AVERAGE_VOLTAGE:
                            return new IntegerReadChannel(mk1, channelId);
                    }
                    return null;
                })
        ).flatMap(channel -> channel);
    }
}