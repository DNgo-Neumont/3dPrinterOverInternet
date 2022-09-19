package dngo.raspberry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fazecast.jSerialComm.SerialPort;

public class GcodeProcessor {
    File gCodeFile;

    BufferedReader gcodeReader;

    BufferedReader portReader;

    BufferedWriter portWriter;

    SerialPort printerPort;

    long gcodeLineCount = 0;

    long currentLineNumber = 0;

    //call first
    public void setGcodeFile(File file){
        gCodeFile = file;
        try {
            gcodeReader = new BufferedReader(new FileReader(gCodeFile));
            gcodeLineCount = Files.lines(gCodeFile.toPath()).count();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //call second
    public void setPort(SerialPort printerPort){
        this.printerPort = printerPort;

        portReader = new BufferedReader(new InputStreamReader(printerPort.getInputStream()));

        portWriter = new BufferedWriter(new OutputStreamWriter(printerPort.getOutputStream()));

    }

    public void processAndSend() throws IOException{
        
        while(currentLineNumber != gcodeLineCount){
            String currentGcodeLine = gcodeReader.readLine();
            currentLineNumber++;
            double currentPercentage = currentLineNumber / gcodeLineCount;
            currentPercentage = currentPercentage * 100;
            System.out.println(currentGcodeLine);

            System.out.println("Line " + currentLineNumber + " of " + gcodeLineCount + "; " + currentPercentage + "% complete");

            while((currentGcodeLine.isBlank() || currentGcodeLine.charAt(0) == ';')){
                currentGcodeLine = gcodeReader.readLine();
                currentLineNumber++;
                currentPercentage = currentLineNumber / gcodeLineCount;
                currentPercentage = currentPercentage * 100;
                System.out.println(currentGcodeLine);
                System.out.println("Line " + currentLineNumber + " of " + gcodeLineCount + "; " + currentPercentage + "% complete");

            }

            if(currentGcodeLine.substring(0, 2) == "G1" || currentGcodeLine.substring(0, 2) ==  "G0"){
                portWriter.write(currentGcodeLine);
                portWriter.newLine();
                portWriter.flush();
                while(portReader.ready()){
                    String printerResponse = portReader.readLine().strip();
                    Pattern matchCoordResponse = Pattern.compile("(X:\\d\\.?\\d{0,20} Y:\\d\\.?\\d{0,20} Z:\\d\\.?\\d{0,20} E:\\d\\.?\\d{0,20})");
                    Matcher regexMatch = matchCoordResponse.matcher(printerResponse);
                    System.out.println("Printer response: " + printerResponse);


                    if(printerResponse.contentEquals("ok")){
                        System.out.println("OK received");
                        break;
                    }else if(regexMatch.find()){
                        String returnedPosition = regexMatch.group(0);
                        if(!currentGcodeLine.substring(2,  currentGcodeLine.length()).contentEquals(returnedPosition)){
                            portWriter.write(currentGcodeLine);
                            portWriter.newLine();
                            portWriter.flush();
                        }
                    }else{
                        portWriter.write("M114"); // Gcode to get head position
                        portWriter.newLine();
                        portWriter.flush();
                    }

                }
            }else{
                portWriter.write(currentGcodeLine);
                portWriter.newLine();
                portWriter.flush();
                while(portReader.ready()){
                    String printerResponse = portReader.readLine().strip();
                    System.out.println("Printer response: " + printerResponse);
                    break;
                }
            }

        }
    }

}
