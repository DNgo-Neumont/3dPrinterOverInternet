package dngo.raspberry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class GcodeListener implements SerialPortDataListener{


    File gCodeFile;

    BufferedReader gcodeReader;

    BufferedReader portReader;

    SerialPort port;

    byte[] bResponse;

    byte[] compileResponse = {" ".getBytes()[0], " ".getBytes()[0]};

    //gonna set up some test code to pull from a file and then we'll work out the rest. Should be fine to have a private file to step through and send
    //commands that way. Gonna be memory intensive, though, and I'd rather find something that's a little more forgiving due to our small memory budget.

    //Maybe have a seperate machine handle parsing the gcode out into messages?

    String currentLine = "";

    //call first
    public void setGcodeFile(File file){
        gCodeFile = file;
        try {
            gcodeReader = new BufferedReader(new FileReader(gCodeFile));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //call second
    public void setPort(SerialPort port){
        this.port = port;

        portReader = new BufferedReader(new InputStreamReader(port.getInputStream()));

    }

    //finally call and let it run
    public void sendFirst() throws IOException{
        currentLine = gcodeReader.readLine();

        currentLine = currentLine + "\n";

        System.out.println(currentLine);

        byte[] bytes = currentLine.getBytes(StandardCharsets.UTF_8);

        port.writeBytes(bytes, bytes.length);
    }

    public boolean finishedPrinting(){
        return currentLine == null;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    //Ok so this works
    //thing is sometimes the printer will have a message split
    //SO
    //Gonna have to change over to readline()
    //so messages don't get torn in half
    @Override
    public void serialEvent(SerialPortEvent event) {
        System.out.print("Printer echoback: ");

        bResponse = event.getReceivedData();

        String response = "";

        //absolutely disgusting.
        //but if it works...
        if(bResponse.length != 1){
            response = new String(bResponse);
        }else{
            if(compileResponse[0] == " ".getBytes()[0]){
                compileResponse[0] = bResponse[0];
            }else{
                compileResponse[1] = bResponse[0];
            }
        }

        if(compileResponse[0] != " ".getBytes()[0] && compileResponse[1] != " ".getBytes()[0]){
            response = new String(compileResponse);
        }

        //I hate this but I can't get this to throw exceptions proper for some reason.
        //AND I'm having issues actually getting the sucker to read.
        //Scratching this. The port refuses to cooperate proper with a input stream and so I'm just going to do a
        //GROSS system of compiling a message together in case of a single letter back.

        // String response = "";
        // try {
        //     response = portReader.readLine();
        // } catch (IOException e1) {
        //     // TODO Auto-generated catch block
        //     e1.printStackTrace();
        // }


        System.out.println(response);

        if(response.contains("ok")){
            try {
                currentLine = gcodeReader.readLine();

                currentLine = currentLine + "\n";

                byte[] bytes = currentLine.getBytes(StandardCharsets.UTF_8);

                System.out.println("Wrote " + port.writeBytes(bytes, bytes.length) + " bytes");
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


    }
    
}
