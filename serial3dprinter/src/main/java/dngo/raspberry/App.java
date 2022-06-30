package dngo.raspberry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Hello world!
 *
 */


 import com.fazecast.jSerialComm.*;
public class App 
{
    public static void main( String[] args )
    {
        SerialPort[] ports = SerialPort.getCommPorts();

        
        
        System.out.println("Select the port you wish to use.");        
        for(int i = 0; i< ports.length; i++){
            System.out.println( i + 1 + ". " + ports[i].getDescriptivePortName());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        int choice = 0;

        try {
            choice = Integer.parseInt(reader.readLine()) - 1;
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final SerialPort portSelected = ports[choice];

        portSelected.setBaudRate(250000);
        portSelected.openPort();
        portSelected.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        //Okay so this works but there is some serious shenanigans
        //First - the printer has to actually initialize the connection
        //Second - all this lets me do so far is read some stuff back from the port.
        //BUT
        //This should let me actually build a writer proper now without dealing with the weirdness of while loops.
        //Tying this into while a GCODE file still has lines to read should allow me to make a system that just feeds GCODE in when the printer sends me an OK response.
        //Just gotta set up the listener to do so.
        portSelected.addDataListener(new SerialPortDataListener() {

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if(event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE){
                    return;
                }    

                byte[] dataRec = new byte[portSelected.bytesAvailable()];

                int read = portSelected.readBytes(dataRec, dataRec.length);

                System.out.println(new String(dataRec));
                
                //BufferedReader portReader = new BufferedReader(new InputStreamReader(portSelected.getInputStream()));

            }
            
        });
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(portSelected.bytesAvailable());

        String response = "G28";

        portSelected.writeBytes(response.getBytes(), response.getBytes().length);

        portSelected.flushIOBuffers();

        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // try{
        //     String lineContent;
        //     while((lineContent = portReader.readLine()) != null && !lineContent.isEmpty() && portSelected.bytesAvailable() > 0){//((lineContent = portReader.readLine()) != null && !lineContent.equals("\n")){
        //         System.out.println(lineContent);
        //     }
        // } catch(Exception e){
        //     e.printStackTrace();
        // }
        portSelected.closePort();

        System.out.println( "Hello World!" );
    }
}
