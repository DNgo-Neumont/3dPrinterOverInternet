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
import java.time.LocalTime;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.MILLIS;

import java.util.ArrayList;

import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.fazecast.jSerialComm.SerialPort;

public class GcodeProcessor implements Runnable{
    File gCodeFile;

    boolean stop = false;

    BufferedReader gcodeReader;

    BufferedReader portReader;
    
    BufferedWriter portWriter;

    SerialPort printerPort;

    long gcodeLineCount = 0;

    public long getGcodeLineCount() {
        return gcodeLineCount;
    }

    public void setGcodeLineCount(long gcodeLineCount) {
        this.gcodeLineCount = gcodeLineCount;
    }

    long currentLineNumber = 0;

    public long getCurrentLineNumber() {
        return currentLineNumber;
    }

    public void setCurrentLineNumber(long currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
    }

    String definedName = "";

    List<String> bufferHistory = new ArrayList<>();
    //Buffer and buffer history size, respectively
    int bufferSize = 3;
    int printBufferLines = 0;
    int historySize = 20;

    @Override
    public void run() {
        try {
            processAndSend();
        } catch (Exception e) {
            System.err.println("Error running gcode for printer: ");
            e.printStackTrace();
        }
        
    }

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

    public String getPortName(){
        return printerPort.getDescriptivePortName();
    }

    public void handleHeatAndCool(String currentGcodeLine, String extruderOrBed) throws IOException{
        portWriter.write(currentGcodeLine);
        portWriter.newLine();
        portWriter.flush();
        boolean bedWarm = false;
        // Get a timestamp of the current time for breaking out of the loop. 
        LocalTime currentTime = LocalTime.now();
        if(currentGcodeLine.contains("S0")){ //For cooling commands so we don't get stuck in a infinite loop, because 0 degrees is unreachable
            bedWarm = true;
        }

        while(!bedWarm){
            String printerResponse = "";
            if(portReader.ready()){
                printerResponse = portReader.readLine().strip();
                // System.out.println("Printer response: " + printerResponse);
            }
            Pattern bedTempResponsePattern = Pattern.compile("("+ extruderOrBed +":\\d{1,10}\\.?\\d{1,10} /\\d{1,10}\\.?\\d{1,10})", Pattern.MULTILINE);
            Matcher bedTempMatcher = bedTempResponsePattern.matcher(printerResponse);
            if(bedTempMatcher.find()){
                String currentTempString = bedTempMatcher.group(0);
                String[] splitString = currentTempString.split(" ");
                float currentTempFloat = Float.parseFloat(splitString[0].substring(2, splitString[0].length()));//for excluding B:                
                float desiredTemp = Float.parseFloat(splitString[1].substring(1, splitString[1].length()));//for excluding a /
                if(desiredTemp < 20){ // around room temp in centigrade
                    portWriter.write(currentGcodeLine);
                    portWriter.newLine();
                    portWriter.flush();
                }
                
                if(currentTempFloat >= desiredTemp - 1.00 && currentTempFloat < desiredTemp + 1.00){
                    bedWarm = true;
                }
            }else if(SECONDS.between(currentTime, LocalTime.now()) > 3){
                currentTime = LocalTime.now();
                portWriter.write("M105");// Report temperatures
                portWriter.newLine();
                portWriter.flush();
                //While loop will handle the rest if this is a preheated bed.
            }
        }
    }

    public double reportStatus(){
        if(currentLineNumber == 0 && gcodeLineCount == 0 ){
            return 0.00;
        }else{
            return currentLineNumber * 100.00 / gcodeLineCount;
        }
    }


    public void processAndSend() throws IOException{
        
        while(currentLineNumber != gcodeLineCount){

            if(stop){
                break;
            }

            String currentGcodeLine = gcodeReader.readLine();
            currentLineNumber++;
            // double currentPercentage = currentLineNumber * 100.00 / gcodeLineCount;
            // System.out.println("current gcode line: " + currentGcodeLine);
            // System.out.println("Line " + currentLineNumber + " of " + gcodeLineCount + "; " + currentPercentage + "% complete");
            
            //Filters out blank lines, comments, and the auto leveling command.
            while((currentGcodeLine.isBlank() || currentGcodeLine.charAt(0) == ';')){ //|| currentGcodeLine.substring(0, 4).contentEquals("M420"))){
                

                currentGcodeLine = gcodeReader.readLine();
                currentLineNumber++;
                if(currentLineNumber >= gcodeLineCount) break;
                // currentPercentage = currentLineNumber * 100.00 / gcodeLineCount;
                // System.out.println(currentGcodeLine);
                // System.out.println("Line " + currentLineNumber + " of " + gcodeLineCount + "; " + currentPercentage + "% complete");

            }
            
            if(currentGcodeLine == null) break;

            if(bufferHistory.size() < historySize){
                bufferHistory.add(currentGcodeLine);
            }else{
                bufferHistory.remove(0);
                bufferHistory.add(currentGcodeLine);
            }

            
            

            //NOTE: does not follow DRY philosophy but it works so
            //Will clean up later and extract into a seperate method
            //Cleaned up.
            if(currentGcodeLine.contains("M190")){ // bed temp warm command
                // System.out.println("Stepped into M190 statement");
                handleHeatAndCool(currentGcodeLine, "B");
            }
            if(currentGcodeLine.contains("M104") || currentGcodeLine.contains("M109")){ // extruder warm command
                // System.out.println("Current gcode line: ");
                // System.out.println("Stepped into m104 or m109 statement");
                handleHeatAndCool(currentGcodeLine, "T");
            }
            if(currentGcodeLine.contains("G28")){
                LocalTime G28startTime = LocalTime.now();
                boolean sent = false;
                while(SECONDS.between(G28startTime,LocalTime.now()) < 5){
                    if(!sent){
                        portWriter.write(currentGcodeLine);
                        portWriter.newLine();
                        portWriter.flush();
                        // System.out.println("Waiting for home command to finish");
                        sent = true;
                    }else if(portReader.ready()){
                        // System.out.println("Printer response: " + portReader.readLine().strip());
                    }
                }
            }
            
            // System.out.println("Current print buffer size: " + printBufferLines);

            if(printBufferLines < bufferSize){
                boolean okToContinue = false;
                boolean sent = false;
                LocalTime timeStamp = LocalTime.now(); //Should not have any null issues - we always set this when we step into the while loop.
                while(!okToContinue){
                    if(!sent){
                        printBufferLines++;
                        portWriter.write(currentGcodeLine);
                        portWriter.newLine();
                        portWriter.flush();
                    }
                    //System.out.println("Stepped into okToContinue loop");
                    String printerResponse = portReader.readLine();

                    // System.out.println("Printer response: " + printerResponse);
                    
                    if(printerResponse.contains("Unknown command: ")){
                        // System.out.println("Buffer contents:");
                        // System.out.println(bufferHistory);
                        String strippedResponse = printerResponse.replace("echo:Unknown command: ", "").strip();
                        strippedResponse = strippedResponse.replace("\"", "");
                        strippedResponse = strippedResponse.replace("G", "");
                        strippedResponse = strippedResponse.replace("M", "");
                        // System.out.println("Error response: "  + strippedResponse);
                        boolean commandFound = false;
                        for (String command : bufferHistory) {
                            if(strippedResponse.contains("M420")){
                                //Some printers perform an autohome command automatically for some reason;
                                //Without this break we'll get hung up on it and tell the printer to home several times before continuing
                                break;
                            }
                            if(command.contains(strippedResponse)){
                                // System.out.println("Command found: " + command);
                                portWriter.write(command);
                                portWriter.newLine();
                                portWriter.flush();
                                printBufferLines++;
                                commandFound = true;
                                LocalTime wait = LocalTime.now();
                                // boolean sentWaitMSG = false;
                                while(MILLIS.between(wait, LocalTime.now()) < 200){
                                    // if(!sentWaitMSG){
                                    //     System.out.println("Waiting on printer to catch up with commands and redo the errored command");
                                    //     sentWaitMSG = true;
                                    // }
                                }
                                break;
                            }

                        }

                        if(!commandFound){
                            LocalTime wait = LocalTime.now();
                            // boolean sentWaitMSG = false;
                            while(MILLIS.between(wait, LocalTime.now()) < 200){
                                // if(!sentWaitMSG){
                                    // System.out.println("Waiting on printer to catch up with commands");
                                    // sentWaitMSG = true;
                                // }
                            }
                            // System.out.println("Unknown command not found; continuing");
                        }
                    }else if(printerResponse.contains("ok")){
                        okToContinue = true;
                        printBufferLines--;
                    }else if(SECONDS.between(timeStamp, LocalTime.now()) > 5){
                        // System.out.println("Last command sent 5 seconds ago");
                        // System.out.println("continuing loop");
                        okToContinue = true;
                        printBufferLines--;
                    }
                } 
            }else{
                LocalTime timeStamp = LocalTime.now();
                while(portReader.ready()){
                    String printerResponse = portReader.readLine().strip();

                    // System.out.println("Waiting on buffer being free; printer response is " + printerResponse);
                    
                    if(printerResponse.contains("Unknown command: ")){
                        // System.out.println("Buffer contents:");
                        // System.out.println(bufferHistory);
                        String strippedResponse = printerResponse.replace("echo:Unknown command: ", "").strip();
                        strippedResponse = strippedResponse.replace("\"", "");
                        // System.out.println("Error response: "  + strippedResponse);
                        boolean commandFound = false;
                        for (String command : bufferHistory) {
                            if(strippedResponse.contains("M420")){
                                //Some printers perform an autohome command automatically for some reason;
                                //Without this break we'll get hung up on it and tell the printer to home several times before continuing
                                break;
                            }
                            if(command.contains(strippedResponse)){
                                // System.out.println("Command found: " + command);
                                portWriter.write(command);
                                portWriter.newLine();
                                portWriter.flush();
                                printBufferLines++;
                                commandFound = true;
                                LocalTime wait = LocalTime.now();
                                boolean sentWaitMSG = false;
                                while(MILLIS.between(wait, LocalTime.now()) < 200){
                                    if(!sentWaitMSG){
                                        // System.out.println("Waiting on printer to catch up with commands and redo the errored command");
                                        sentWaitMSG = true;
                                    }
                                }
                                break;
                            }

                        }

                        if(!commandFound){
                            LocalTime wait = LocalTime.now();
                            // boolean sentWaitMSG = false;
                            while(MILLIS.between(wait, LocalTime.now()) < 200){
                                // if(!sentWaitMSG){
                                //     System.out.println("Waiting on printer to catch up with commands");
                                //     sentWaitMSG = true;
                                // }
                            }
                            // System.out.println("Unknown command not found; continuing");
                        }
                    }
                    if(SECONDS.between(timeStamp, LocalTime.now()) > 3){
                        break;
                    }
                }
                printBufferLines--;
            }
        }
    }

    public String getDefinedName(){
        return definedName;
    }

    public void setDefinedName(String newName){
        definedName = newName;
    }

    public void requestStop(){
        stop = true;
    }

}
