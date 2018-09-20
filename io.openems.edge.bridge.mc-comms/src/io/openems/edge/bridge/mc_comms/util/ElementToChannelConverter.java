package io.openems.edge.bridge.mc_comms.util;

import java.util.function.Function;

public class ElementToChannelConverter<elementType, channelType> {

    private final Function<elementType, channelType> elementToChannel;
    private final Function<channelType, elementType> channelToElement;

    /**
     * Converts directly 1-to-1 between Element and Channel
     */
    public final static ElementToChannelConverter DIRECT_1_TO_1 = new ElementToChannelConverter( //
            // element -> channel
            value -> value, //
            // channel -> element
            value -> value);

    /**
     * Applies a scale factor of -1.
     *
     * @see ElementToChannelScaleFactorConverter
     */
    public final static ElementToChannelConverter SCALE_FACTOR_MINUS_1 = new ElementToChannelScaleFactorConverter(-1);

    /**
     * Applies a scale factor of 1.
     *
     * @see ElementToChannelScaleFactorConverter
     */
    public final static ElementToChannelConverter SCALE_FACTOR_1 = new ElementToChannelScaleFactorConverter(1);

    /**
     * Applies a scale factor of 2.
     *
     * @see ElementToChannelScaleFactorConverter
     */
    public final static ElementToChannelConverter SCALE_FACTOR_2 = new ElementToChannelScaleFactorConverter(2);

    /**
     * Applies a scale factor of 3.
     *
     * @see ElementToChannelScaleFactorConverter
     */
    public final static ElementToChannelConverter SCALE_FACTOR_3 = new ElementToChannelScaleFactorConverter(3);

    /**
     * This constructs and back-and-forth converter from Element to Channel and back
     *
     * @param elementToChannel
     * @param channelToElement
     */
    public ElementToChannelConverter(Function<elementType, channelType> elementToChannel,
                                     Function<channelType, elementType> channelToElement) {
        this.elementToChannel = elementToChannel;
        this.channelToElement = channelToElement;
    }

    /**
     * This constructs a forward-only converter from Element to Channel.
     * Back-conversion throws an Exception.
     *
     * @param elementToChannel
     */
    public ElementToChannelConverter(Function<elementType, channelType> elementToChannel) {
        this.elementToChannel = elementToChannel;
        this.channelToElement = (value) -> {
            throw new IllegalArgumentException("Backwards-Conversion for [" + value + "] is not implemented.");
        };
    }

    /**
     * Convert an Element value to a Channel value. If the value can or should not
     * be converted, this method returns null.
     *
     * @param value
     * @return the converted value or null
     */
    public Object elementToChannel(elementType value) {
        return this.elementToChannel.apply(value);
    }

    public Object channelToElement(channelType value) {
        return this.channelToElement.apply(value);
    }
}
