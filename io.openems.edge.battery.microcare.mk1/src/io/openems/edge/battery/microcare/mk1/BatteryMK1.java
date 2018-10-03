package io.openems.edge.battery.microcare.mk1;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.api.element.*;
import io.openems.edge.bridge.mc_comms.api.task.ReadMCCommsTask;
import io.openems.edge.bridge.mc_comms.util.AbstractMCCommsComponent;
import io.openems.edge.bridge.mc_comms.util.ElementToChannelConverter;
import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.util.concurrent.atomic.AtomicReference;


@Designate( ocd=Config.class, factory=true)
@Component(
		name="io.openems.edge.battery.microcare.mk1",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class BatteryMK1 extends AbstractMCCommsComponent implements Battery, OpenemsComponent {

	private String bridgeID;
	private final int READ_COMMAND_1 = 1;
	private final int EXPECTED_REPLY_1 = 1;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMCCommsBridge(MCCommsBridge bridge) {
		super.setMCCommsBridge(bridge);
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		AVERAGE_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		AVERAGE_CURRENT(new Doc().unit(Unit.MILLIAMPERE));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	@Override
	protected MCCommsProtocol defineMCCommsProtocol() {
		return new MCCommsProtocol(new AtomicReference<>(this),
				new ReadMCCommsTask(READ_COMMAND_1, EXPECTED_REPLY_1, Priority.HIGH,
						m(Battery.ChannelId.BATTERY_TEMP, new SignedInt8BitElement(0), ElementToChannelConverter.SCALE_FACTOR_0),
						m(Battery.ChannelId.CAPACITY_KWH, new Integer32BitElement(1), ElementToChannelConverter.SCALE_FACTOR_0),
						m(Battery.ChannelId.READY_FOR_WORKING, new BooleanElement(5)),
						m(Battery.ChannelId.SOC, new Integer8BitElement(6), ElementToChannelConverter.SCALE_FACTOR_0),
						m(Battery.ChannelId.SOH, new Integer8BitElement(7), ElementToChannelConverter.SCALE_FACTOR_0),
						m(ChannelId.AVERAGE_CURRENT, new Integer16BitElement(8), ElementToChannelConverter.SCALE_FACTOR_1),
						m(ChannelId.AVERAGE_VOLTAGE, new Integer16BitElement(10), ElementToChannelConverter.SCALE_FACTOR_1)
				),
				new ReadMCCommsTask(READ_COMMAND_1, EXPECTED_REPLY_1, Priority.LOW,
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new Integer16BitElement(0), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new Integer16BitElement(2), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new Integer16BitElement(4), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new Integer16BitElement(6), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.MAX_CAPACITY, new Integer32BitElement(8), ElementToChannelConverter.SCALE_FACTOR_0))
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), cm, config.slaveAddress(), config.bridgeID());
		bridgeID = config.bridgeID();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

}
