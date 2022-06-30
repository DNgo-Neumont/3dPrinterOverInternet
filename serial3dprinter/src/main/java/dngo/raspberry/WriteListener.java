package dngo.raspberry;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class WriteListener implements SerialPortDataListener{

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_WRITTEN;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if(event.getEventType() == SerialPort.LISTENING_EVENT_DATA_WRITTEN){
            System.out.println("Bytes written to port");
        }
        
    }
    
}