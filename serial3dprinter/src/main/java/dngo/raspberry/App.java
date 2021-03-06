package dngo.raspberry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
        //Okay so this works but there is some serious shenanigans
        //First - the printer has to actually initialize the connection
        //Second - all this lets me do so far is read some stuff back from the port.
        //BUT
        //This should let me actually build a writer proper now without dealing with the weirdness of while loops.
        //Tying this into while a GCODE file still has lines to read should allow me to make a system that just feeds GCODE in when the printer sends me an OK response.
        //Just gotta set up the listener to do so.
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //portSelected.addDataListener(new MessageListener());
        
        //System.out.println(portSelected.bytesAvailable());
        
        
        // byte[] bytes = new byte[portSelected.bytesAvailable()];
        // portSelected.readBytes(bytes, bytes.length);
        
        // System.out.println(new String(bytes));

        BufferedReader portReader = new BufferedReader(new InputStreamReader(portSelected.getInputStream()));
        //BufferedWriter portWriter = new BufferedWriter(new OutputStreamWriter(portSelected.getOutputStream()));

        portSelected.addDataListener(new MessageListener());

        portSelected.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 8000, 8000);
        
        String response = "";
        
        while(!response.equals("exit")){
            try {
                response = reader.readLine();
                // System.out.println("Port open: " + portSelected.isOpen());
                // System.out.println("Port read buffer size: " + portSelected.getDeviceReadBufferSize());
                // System.out.println("Port write buffer size: " + portSelected.getDeviceWriteBufferSize());
                //System.out.println("Clearing RTS signal - because why not");
                //portSelected.clearRTS();
                // System.out.println("RTS signal is " + portSelected.getRTS());
                // System.out.println(portSelected.getFlowControlSettings());
                //Would rather not use this method - "borrowed" from stackOverflow at https://stackoverflow.com/questions/5688042/how-to-convert-a-java-string-to-an-ascii-byte-array
                //But if it's what I need to do to get the printer to even read what the hell I sent it it's good enough for now.
                //turns out I don't need it - the issue wasn't the encoding but the message
                //The printer will only accept a response ending with \n - even the standard echoback about an unrecognized command doesn't come through
                //now to automate and send a gcode file through to have a proper print
                byte[] responseBytes = new String(response + "\n").getBytes(StandardCharsets.US_ASCII);

                System.out.println("Response bytes: " + new String(responseBytes));
                System.out.println("Wrote " + portSelected.writeBytes(responseBytes, responseBytes.length) + " bytes");
                // System.out.println("Bytes waiting to be written: " + portSelected.bytesAwaitingWrite());
                // while(portReader.ready()){
                //     System.out.println(portReader.readLine());
                // }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // System.out.println("Bytes to be written: " + portSelected.bytesAwaitingWrite());
        // System.out.println("Write timeout: " + portSelected.getWriteTimeout());
        // System.out.println("Write buffer size: " + portSelected.getDeviceWriteBufferSize());

        System.out.println(portSelected.bytesAwaitingWrite());

        try {
            while(portReader.ready()){
                System.out.println(portReader.readLine());
            }
            portReader.close();
        } catch (IOException e1) {
            e1.printStackTrace();
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
