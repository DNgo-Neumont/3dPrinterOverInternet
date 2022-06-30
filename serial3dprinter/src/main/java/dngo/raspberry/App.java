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

        SerialPort portSelected = ports[choice];

        portSelected.setBaudRate(250000);
        portSelected.openPort();
        portSelected.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        BufferedReader portReader = new BufferedReader(new InputStreamReader(portSelected.getInputStream()));
        BufferedWriter portWriter = new BufferedWriter(new OutputStreamWriter(portSelected.getOutputStream()));
        
        try{
            String lineContent;
            while((lineContent = portReader.readLine()) != null && !lineContent.equals("\n")){
                System.out.println(lineContent);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        portSelected.closePort();

        System.out.println( "Hello World!" );
    }
}
