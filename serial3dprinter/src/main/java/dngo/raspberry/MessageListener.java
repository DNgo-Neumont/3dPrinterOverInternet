package dngo.raspberry;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class MessageListener implements SerialPortDataListener {


    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {

        System.out.println("Data received!");

        System.out.println(event.getEventType());

        System.out.println(event.getSerialPort().getDescriptivePortName());

        System.out.println(event.getReceivedData());

        byte[] dataRec = event.getReceivedData();

        System.out.println(new String(dataRec));
        
        //BufferedReader portReader = new BufferedReader(new InputStreamReader(portSelected.getInputStream()));

    }
    
    
}
