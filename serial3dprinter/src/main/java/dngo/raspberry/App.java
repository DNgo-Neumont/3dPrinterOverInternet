package dngo.raspberry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

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
        portSelected.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED);
        //Okay so this works but there is some serious shenanigans
        //First - the printer has to actually initialize the connection
        //Second - all this lets me do so far is read some stuff back from the port.
        //BUT
        //This should let me actually build a writer proper now without dealing with the weirdness of while loops.
        //Tying this into while a GCODE file still has lines to read should allow me to make a system that just feeds GCODE in when the printer sends me an OK response.
        //Just gotta set up the listener to do so.
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        portSelected.addDataListener(new MessageListener());
        
        //System.out.println(portSelected.bytesAvailable());
        
        
        // byte[] bytes = new byte[portSelected.bytesAvailable()];
        // portSelected.readBytes(bytes, bytes.length);
        
        // System.out.println(new String(bytes));
        BufferedWriter portWriter = new BufferedWriter(new PrintWriter(portSelected.getOutputStream()));

        portSelected.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 4000, 4000);
        
        

        String response = "";
        
        while(!response.equals("exit")){
            try {
                response = reader.readLine();
                portWriter.write(response, 0, response.length());
                System.out.println("Bytes to write: " + portSelected.bytesAwaitingWrite());
                portSelected.flushIOBuffers();
                // byte[] responseBytes = response.getBytes();
                // System.out.println("Wrote " + portSelected.writeBytes(responseBytes, responseBytes.length) + " bytes");
                // portSelected.flushIOBuffers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // System.out.println("Bytes to be written: " + portSelected.bytesAwaitingWrite());
        // System.out.println("Write timeout: " + portSelected.getWriteTimeout());
        // System.out.println("Write buffer size: " + portSelected.getDeviceWriteBufferSize());

        System.out.println(portSelected.bytesAwaitingWrite());

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
