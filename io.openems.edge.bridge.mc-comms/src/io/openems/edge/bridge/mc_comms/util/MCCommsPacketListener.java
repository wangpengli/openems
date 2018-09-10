package io.openems.edge.bridge.mc_comms.util;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;
import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class MCCommsPacketListener implements SerialPortPacketListener {

    private final Logger logger = LoggerFactory.getLogger(MCCommsPacketListener.class);
    private final int sourceAddress;
    private final int destinationAddress;
    private Consumer<MCCommsRXPacket> onReadCallback;


    public MCCommsPacketListener(int sourceAddress, MCCommsBridge bridge) {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = bridge.getMasterAddress();
        bridge.getSerialPort().addDataListener(this);
    }

    public void addOnReadCallback(Consumer<MCCommsRXPacket> onReadCallback) {
        this.onReadCallback = onReadCallback;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        try {
            MCCommsRXPacket rxPacket = new MCCommsRXPacket(serialPortEvent.getReceivedData());
            if ((rxPacket.getSourceAddress() == this.sourceAddress) &&
                    (rxPacket.getDestinationAddress() == this.destinationAddress)) {
                this.onReadCallback.accept(rxPacket);
            }
        } catch (MCCommsException e) {
            logger.debug(e.getMessage());
        }

    }

    @Override
    public int getPacketSize() {
        return 25;
    }

}
