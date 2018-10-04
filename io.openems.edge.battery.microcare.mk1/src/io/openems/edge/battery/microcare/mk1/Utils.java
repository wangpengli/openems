package io.openems.edge.battery.microcare.mk1;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.Arrays;
import java.util.stream.Stream;

public class Utils {
    public static Stream<? extends AbstractReadChannel<?>> initializeChannels(BatteryMK1 c) {
        return Stream.of( //
                Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
                    switch (channelId) {
                        case STATE:
                            return new StateChannel(c, channelId);
                    }
                    return null;
                }), Arrays.stream(Battery.ChannelId.values()).map(channelId -> {
                    switch (channelId) {
                        case BATTERY_TEMP:
                        case SOH:
                        case SOC:
                        case CAPACITY_KWH:
                        case CHARGE_MAX_CURRENT:
                        case CHARGE_MAX_VOLTAGE:
                        case DISCHARGE_MAX_CURRENT:
                        case DISCHARGE_MIN_VOLTAGE:
                        case MAX_CAPACITY:
                            return new IntegerReadChannel(c, channelId);
                    }
                    return null;
                }), Arrays.stream(Battery.ChannelId.values()).map(channelId -> {
                    switch (channelId) {
                        case READY_FOR_WORKING:
                            return new BooleanReadChannel(c, channelId);
                    }
                    return null;
                })
        ).flatMap(channel -> channel);
    }
}