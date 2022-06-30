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
        portSelected.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
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
