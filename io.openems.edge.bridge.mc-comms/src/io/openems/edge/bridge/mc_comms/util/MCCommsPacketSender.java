package io.openems.edge.bridge.mc_comms.util;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import io.openems.edge.bridge.mc_comms.MCCommsBridge;

import java.io.IOException;
import java.util.function.Consumer;

public class MCCommsPacketSender {

    private final MCCommsBridge bridge;
    private boolean success;

    private synchronized void setSuccess(boolean success) {
        this.success = success;
    }

    public MCCommsPacketSender(MCCommsBridge bridge) {
        this.bridge = bridge;
    }

    public void writePacket(MCCommsTXPacket TXPacket) throws MCCommsException {
        Thread writeConfirm = new Thread() {
            @Override
            public void run() {
                super.run();
                new WriteEventListener(b -> {
                    setSuccess(b);
                    this.notifyAll();
                    this.destroy();
                });
            }
        };
        writeConfirm.run();
        try {
            writeConfirm.wait(100);
            this.bridge.getSerialPort().getOutputStream().write(TXPacket.getPacketBuffer().array());
            if (!success)
                throw new MCCommsException("[MCCOMMS] unable to write to serial bus");
        } catch (InterruptedException | IOException e) {
            throw new MCCommsException(e.getMessage());
        }

    }

    private class WriteEventListener implements SerialPortDataListener {

        private final Consumer<Boolean> callBack;

        WriteEventListener(Consumer<Boolean> callBack) {
            this.callBack = callBack;
        }

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_WRITTEN;
        }

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            this.callBack.accept(true);
        }
    }

}
