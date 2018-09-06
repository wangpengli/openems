package io.openems.edge.bridge.mc_comms;

import com.fazecast.jSerialComm.SerialPort;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
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

import java.util.Arrays;
import java.util.stream.Stream;


@Designate( ocd= Config.class, factory=true)

@Component(name="io.openems.edge.bridge.mc-comms")
public class MCCommsBridge extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

	private final Multimap<String, MCCommsProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());
	private MCCommsWorker worker = new MCCommsWorker(this);
	private String portName = "";
	private SerialPort serialPort;
	private int masterAddress;



	public MCCommsBridge() {
		Stream.of(
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
						case STATE:
							return new StateCollectorChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
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

	public SerialPort getSerialPort() {
		return serialPort;
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
		this.serialPort = SerialPort.getCommPort(config.portName());
		this.serialPort.setComPortParameters(9600, 8, 0, 0);
		this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 25, 0);
		this.masterAddress = config.masterAddress();
		this.serialPort.openPort();
	}

	@Deactivate
	protected void deactivate() {
		this.worker.deactivate();
		this.serialPort.closePort();
		super.deactivate();
	}



}
