package io.openems.edge.bridge.mc_comms;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.openems.edge.bridge.mc_comms.util.MCCommsPacketBuffer;
import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.bridge.mc_comms.util.MCCommsWorker;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Stream;


@Designate( ocd= Config.class, factory=true)

@Component(name="io.openems.edge.bridge.mc-comms")
public class MCCommsBridge extends AbstractOpenemsComponent implements OpenemsComponent {

	private final Multimap<String, MCCommsProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());
	private final Logger logger = LoggerFactory.getLogger(MCCommsBridge.class);
	private MCCommsWorker worker = new MCCommsWorker(this.protocols);
	private int masterAddress;
	private MCCommsPacketBuffer IOPacketBuffer = new MCCommsPacketBuffer();


	public MCCommsBridge() {
		Stream.of(
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
						case STATE:
							return new StateCollectorChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(this::addChannel);
	}

	/**
	 * Adds the protocol
	 *
	 * @param sourceId
	 * @param protocol
	 */
	public void addProtocol(String sourceId, MCCommsProtocol protocol) {
		this.protocols.put(sourceId, protocol);
	}

	/**
	 * Removes the protocol
	 */
	public void removeProtocol(String sourceId) {
		this.protocols.removeAll(sourceId);
	}


	public Multimap<String, MCCommsProtocol> getProtocols() {
		return protocols;
	}

	public int getMasterAddress() {
		return masterAddress;
	}

	@Override
	public String debugLog() {
		return null;
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.masterAddress = config.masterAddress();
		this.getIOPacketBuffer().start(config.portName());
		if (this.isEnabled()) {
			this.worker.activate(config.id());
		}
	}

	@Deactivate
	protected void deactivate() {
		this.worker.deactivate();
		this.getIOPacketBuffer().stop();
		super.deactivate();
	}


	public MCCommsPacketBuffer getIOPacketBuffer() {
		return IOPacketBuffer;
	}
}
