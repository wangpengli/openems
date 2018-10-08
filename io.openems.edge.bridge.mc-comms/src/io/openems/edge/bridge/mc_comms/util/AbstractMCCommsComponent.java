package io.openems.edge.bridge.mc_comms.util;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.api.BridgeMCComms;
import io.openems.edge.bridge.mc_comms.api.element.BooleanCollectiveElement;
import io.openems.edge.bridge.mc_comms.api.element.BooleanElement;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class AbstractMCCommsComponent extends AbstractOpenemsComponent {

    private int slaveAddress;
    private AtomicReference<MCCommsBridge> bridge = new AtomicReference<>();
    private LinkedTransferQueue<MCCommsPacket> transferQueue = new LinkedTransferQueue<>();
    private MCCommsProtocol protocol;
    private ArrayList<MCCommsPacket> awaitingPackets = new ArrayList<>();

    @Override
    protected void activate(ComponentContext context, String service_pid, String id, boolean enabled) {
        throw new IllegalArgumentException("Invalid activate() method call; use other method");
    }

    protected void activate(ComponentContext context, String service_pid, String id, boolean enabled, ConfigurationAdmin configManager, int slaveAddress, String bridgeID) {
        super.activate(context, service_pid, id, enabled);
        if (OpenemsComponent.updateReferenceFilter(configManager, service_pid, "MCCommsBridge", bridgeID))
            return;
        this.slaveAddress = slaveAddress;
        MCCommsBridge bridge = this.bridge.get();
        if (this.isEnabled() && bridge != null)
            bridge.addProtocol(this.id(), this.getMCCommsProtocol());
    }


    protected void setMCCommsBridge(BridgeMCComms bridge) {
        this.bridge.set((MCCommsBridge) bridge);
        this.bridge.get().registerTransferQueue(this.slaveAddress, this.transferQueue);
    }

    public LinkedTransferQueue<MCCommsPacket> getTransferQueue() {
        return transferQueue;
    }

    public MCCommsPacket getNextPacket() {
        transferQueue.drainTo(awaitingPackets);
        int i = awaitingPackets.size() - 1;
        if (i > 0) {
            MCCommsPacket returnPacket = awaitingPackets.get(i);
            awaitingPackets.remove(i);
            return returnPacket;
        } else {
            return null;
        }
    }

    public MCCommsPacket getPacket(int expectedReplyCommand, int timeOutMilliSeconds) {
        transferQueue.drainTo(awaitingPackets);
        for (MCCommsPacket currPacket : awaitingPackets) {
            if (currPacket.command == expectedReplyCommand) {
                return currPacket;
            }
        }
        try {
            MCCommsPacket currPacket = transferQueue.poll(timeOutMilliSeconds, TimeUnit.MILLISECONDS);
            if (currPacket != null && currPacket.command == expectedReplyCommand) {
                return currPacket;
            }
        } catch (InterruptedException e) {
            return null;
        }

        return null;
    }

    public AtomicReference<MCCommsBridge> getMCCommsBridgeAtomicRef() {
        return this.bridge;
    }

    protected MCCommsProtocol getMCCommsProtocol() {
        if (this.protocol != null)
            return this.protocol;
        this.protocol = this.defineMCCommsProtocol();
        return this.protocol;
    }

    protected abstract MCCommsProtocol defineMCCommsProtocol();

    public int getSlaveAddress() {
        return slaveAddress;
    }

    public class ChannelMapper {
        private final MCCommsElement<?> element;
        private Map<Channel<?>, ElementToChannelConverter> channelMaps = new HashMap<>();
        Logger log = LoggerFactory.getLogger(this.getClass());

        public ChannelMapper(MCCommsElement<?> element) {
            this.element = element;
            this.element.onUpdateCallback((value) -> {
                /*
                 * Applies the updated value on every Channel in ChannelMaps using the given
                 * Converter. If the converter returns an Optional.empty, the value is ignored.
                 */
                this.channelMaps.forEach((channel, converter) -> {
                    Object convertedValue;
                    try {
                        convertedValue = converter.elementToChannel(value);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Conversion for [" + channel.channelId() + "] failed", e);
                    }
                    channel.setNextValue(convertedValue);
                });
            });
        }

        public ChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId,
                               ElementToChannelConverter converter) {
            Channel<?> channel = channel(channelId);
            this.channelMaps.put(channel, converter);
            /*
             * handle Channel Write to Element
             */
            if (channel instanceof WriteChannel<?>) {
                ((WriteChannel<?>) channel).onSetNextWrite(value -> {
                    try {
                        Optional convertedValue = Optional.ofNullable(converter.channelToElement(value));
                        element.setNextWriteValue(convertedValue);
                    } catch (OpenemsException e) {
                        log.warn("Unable to write to MCCommsElement [" + this.element.getByteAddress()
                                + "]: " + e.getMessage());
                    }
                });
            }
            return this;
        }

        public ChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId,
                               Function<Object, Object> elementToChannel, Function<Object, Object> channelToElement) {
            ElementToChannelConverter converter = new ElementToChannelConverter(elementToChannel, channelToElement);
            return this.m(channelId, converter);
        }

        public MCCommsElement<?> build() {
            return this.element;
        }
    }

    /**
     * Creates a ChannelMapper that can be used with builder pattern inside the
     * protocol definition.
     *
     * @param element
     * @return
     */
    protected final ChannelMapper cm(MCCommsElement<?> element) {
        return new ChannelMapper(element);
    }

    /**
     * Maps the given element 1-to-1 to the Channel identified by channelId.
     *
     * @param channelId
     * @param element
     * @return the element parameter
     */
    protected final MCCommsElement<?> m(io.openems.edge.common.channel.doc.ChannelId channelId, MCCommsElement<?> element) {
        return new ChannelMapper(element) //
                .m(channelId, ElementToChannelConverter.SCALE_FACTOR_0) //
                .build();
    }

    /**
     * Maps the given element to the Channel identified by channelId, applying the
     * given @link{ElementToChannelConverter}
     *
     * @param channelId
     * @param element
     * @return the element parameter
     */
    protected final MCCommsElement<?> m(io.openems.edge.common.channel.doc.ChannelId channelId, MCCommsElement<?> element, ElementToChannelConverter converter) {
        return new ChannelMapper(element) //
                .m(channelId, converter) //
                .build();
    }

    public class BooleanChannelMapper {
        private final BooleanElement element;
        private Channel<?> channel;

        public BooleanChannelMapper(BooleanElement element) {
            this.element = element;
            this.element.onUpdateCallback((value) -> channel.setNextValue(element.getValue()));
        }

        public BooleanChannelMapper m(ChannelId channelId) {
            this.channel = channel(channelId);
            if (channel.getType() != OpenemsType.BOOLEAN) {
                throw new IllegalArgumentException(
                        "Channel [" + channelId + "] must be of type [BOOLEAN] for bit-mapping.");
            }
            return this;
        }

        public BooleanElement build() {
            return this.element;
        }

    }

    /**
     * Private subclass to handle Channels that are mapping to one bit of a MCComms
     * Unsigned 8-bit element
     */
    public class BitChannelMapper {
        private final BooleanCollectiveElement element;
        private final Map<Integer, Channel<?>> channels = new HashMap<>();

        public BitChannelMapper(BooleanCollectiveElement element) {
            this.element = element;
            this.element.onUpdateCallback((value) -> {
                this.channels.forEach((bitIndex, channel) -> {
                    if (value << ~bitIndex < 0) {
                        channel.setNextValue(true);
                    } else {
                        channel.setNextValue(false);
                    }
                });
            });
        }

        public BitChannelMapper m(ChannelId channelId, int bitIndex) {
            Channel<?> channel = channel(channelId);
            if (channel.getType() != OpenemsType.BOOLEAN) {
                throw new IllegalArgumentException(
                        "Channel [" + channelId + "] must be of type [BOOLEAN] for bit-mapping.");
            }
            this.channels.put(bitIndex, channel);
            return this;
        }

        public BooleanCollectiveElement build() {
            return this.element;
        }
    }

    /**
     * Creates a BitChannelMapper that can be used with builder pattern inside the
     * protocol definition.
     *
     * @param element
     * @return
     */
    protected final BitChannelMapper bcm(BooleanCollectiveElement element) {
        return new BitChannelMapper(element);
    }

    /**
     * Creates a BooleanChannelMapper that can be used with builder pattern inside the
     * protocol definition.
     *
     * @param element
     * @return
     */
    protected final BooleanChannelMapper bm(BooleanElement element) {
        return new BooleanChannelMapper(element);
    }

}
