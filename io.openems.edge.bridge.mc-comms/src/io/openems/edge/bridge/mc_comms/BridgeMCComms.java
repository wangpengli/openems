package io.openems.edge.bridge.mc_comms;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import javafx.event.Event;
import javafx.event.EventHandler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;


@Designate( ocd= Config.class, factory=true)

@Component(name="io.openems.edge.bridge.mc-comms")
public class BridgeMCComms extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

	private final Multimap<String, MCCommsProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	private MCCommsWorker worker = new MCCommsWorker(this, Multimaps.synchronizedListMultimap(ArrayListMultimap.create()));

	private String portName = "";


	public BridgeMCComms() {
		Stream.of(
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
						case STATE:
							return new StateCollectorChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	public Multimap<String, MCCommsProtocol> getProtocols() {
		return protocols;
	}

	@Override
	public String debugLog() {
		return null;
	}

	@Override
	public void handle(Event event) {

	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.portName = config.portName();
		if (this.isEnabled()) {
			this.worker.activate(config.id());
		}
	}

	@Deactivate
	void deactivate() {
	}



}
