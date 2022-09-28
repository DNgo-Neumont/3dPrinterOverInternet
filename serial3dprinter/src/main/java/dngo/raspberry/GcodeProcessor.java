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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
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

    List<String> buffer = new ArrayList<String>();
    List<String> bufferHistory = new ArrayList<>();
    //Buffer and buffer history size, respectively
    int bufferSize = 4;
    int printBufferLines = 0;
    int historySize = 20;

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
                System.out.println("Printer response: " + printerResponse);
            }
            Pattern bedTempResponsePattern = Pattern.compile("("+ extruderOrBed +":\\d{1,10}\\.?\\d{1,10} /\\d{1,10}\\.?\\d{1,10})", Pattern.MULTILINE);
            Matcher bedTempMatcher = bedTempResponsePattern.matcher(printerResponse);
            if(bedTempMatcher.find()){
                String currentTempString = bedTempMatcher.group(0);
                String[] splitString = currentTempString.split(" ");
                float currentTempFloat = Float.parseFloat(splitString[0].substring(2, splitString[0].length()));//for excluding B:
                float desiredTemp = Float.parseFloat(splitString[1].substring(1, splitString[1].length()));//for excluding a /
                
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


    public void processAndSend() throws IOException{
        
        while(currentLineNumber != gcodeLineCount){

            String currentGcodeLine = "";

            while(buffer.size() < 4 && (currentGcodeLine = gcodeReader.readLine()) != null){

                currentLineNumber++;
                double currentPercentage = currentLineNumber / gcodeLineCount;
                currentPercentage = currentPercentage * 100;
                System.out.println(currentGcodeLine);         
                System.out.println("Line " + currentLineNumber + " of " + gcodeLineCount + "; " + currentPercentage + "% complete");
                //Ignores blank / commented lines.
                while((currentGcodeLine.isBlank() || currentGcodeLine.charAt(0) == ';' || currentGcodeLine.contains("M420"))){
                    currentGcodeLine = gcodeReader.readLine();
                    currentLineNumber++;
                    currentPercentage = currentLineNumber / gcodeLineCount;
                    currentPercentage = currentPercentage * 100;
                    System.out.println(currentGcodeLine);
                    System.out.println("Line " + currentLineNumber + " of " + gcodeLineCount + "; " + currentPercentage + "% complete");
    
                }
                
                buffer.add(currentGcodeLine);
                if(bufferHistory.size() < historySize){
                    bufferHistory.add(currentGcodeLine);
                }else{
                    bufferHistory.remove(0);
                    bufferHistory.add(currentGcodeLine);
                }

            }
            
            
            
            
            String bufferLine = "";
            
            try{
                
                bufferLine = buffer.get(0);
            }catch(NoSuchElementException e){
                System.out.println("Print buffer empty");
            }
            
            System.out.println("Current bufferline: " + bufferLine);

            //NOTE: does not follow DRY philosophy but it works so
            //Will clean up later and extract into a seperate method
            //Cleaned up.
            if(bufferLine.contains("M190")){ // bed temp warm command
                handleHeatAndCool(bufferLine, "B");
                buffer.remove(0);
            }
            if(bufferLine.contains("M104") || bufferLine.contains("M109")){ // extruder warm command
                handleHeatAndCool(bufferLine, "T");
                buffer.remove(0);
            }
            
            if(bufferLine.substring(0, 2).contentEquals("G1") || bufferLine.substring(0, 2).contentEquals("G0") 
            || bufferLine.substring(0, 2).contentEquals("G2") || bufferLine.substring(0, 2).contentEquals("G3") 
            && printBufferLines < bufferSize){
                boolean okToContinue = false;
                boolean sent = false;
                LocalTime timeStamp = null; //Should not have any null issues - we always set this when we step into the while loop.
                while(!okToContinue){
                    if(!sent){
                        portWriter.write(bufferLine);
                        portWriter.newLine();
                        portWriter.flush();
                        timeStamp = LocalTime.now();
                    }

                    String printerResponse = portReader.readLine();

                    System.out.println("Printer response: " + printerResponse);
                    
                    if(printerResponse.contains("Unknown command: ")){
                        System.out.println("Buffer contents:");
                        System.out.println(bufferHistory);
                        System.out.println(buffer);
                        String strippedResponse = printerResponse.replace("echo:Unknown command: ", "").strip();
                        strippedResponse = strippedResponse.replace("\"", "");
                        System.out.println("Error response: "  + strippedResponse);
                        boolean commandFound = false;
                        for (String command : bufferHistory) {
                            if(command.contains(strippedResponse)){
                                portWriter.write(command);
                                portWriter.newLine();
                                portWriter.flush();
                                commandFound = true;
                                break;
                            }
                        }

                        if(!commandFound){
                            System.out.println("Unknown command not found; continuing");
                        }

                    }else if(printerResponse.contains("ok")){
                        okToContinue = true;
                        printBufferLines--;
                        System.out.println("First item in buffer removed: " + buffer.remove(0));
                    }else if(SECONDS.between(timeStamp, LocalTime.now()) > 5){
                        System.out.println("Last command sent 5 seconds ago");
                        System.out.println("continuing loop");
                        okToContinue = true;
                        printBufferLines--;
                        System.out.println("First item in buffer removed: " + buffer.remove(0));
                    }
                } 
            //else statement handles all non move gcode
            }else if(printBufferLines < bufferSize){
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                portWriter.write(bufferLine);
                portWriter.newLine();
                portWriter.flush();
                printBufferLines++;
                buffer.remove(0);
                LocalTime timeStamp = LocalTime.now();
                while(portReader.ready()){
                    System.out.println("Printer response: " + portReader.readLine());
                    printBufferLines--;
                    if(SECONDS.between(timeStamp, LocalTime.now()) > 2){
                        break;
                    }
                }

            }
            //     portWriter.write(bufferLine);
            //     portWriter.newLine();
            //     portWriter.flush();
            //     boolean okToContinue = false;
                
            //     //Will change to a bool and drop out once an ok has been recieved - should create a much better flow back and forth
            //     //Didn't work.
            //     //Not sure how to limit this entirely, now
            //     //Considering just writing a loop that listens and reacts accordingly
            //     //Quickly recompiling and pushing to see if I just forgot to send this off to the test rig
                

            //     //Doing more research and went back to that old apple2gs vid running a printer
            //     //He used a buffer. Same issue I've been having with dropped commands. 
            //     //Of course. Will implement a queue and then some handling as having a queue allows lookback on dropped commands. 
            //     //
            //     while(!okToContinue){         
            //         portWriter.write("M114");
            //         portWriter.newLine();
            //         portWriter.flush();
            //         if(portReader.ready()){
            //             LocalTime currentTime = LocalTime.now();
                        
            //             String printerResponse = portReader.readLine().strip();
            //             System.out.println("Printer response: " + printerResponse);
            //             Pattern matchCoordResponse = Pattern.compile("(X:\\d\\.?\\d{0,20} Y:\\d\\.?\\d{0,20} Z:\\d\\.?\\d{0,20} E:\\d\\.?\\d{0,20})");
            //             Matcher responseMatcher = matchCoordResponse.matcher(printerResponse);
            //             boolean xMatch = true;
            //             boolean yMatch = true;
            //             boolean zMatch = true;
            //             if(responseMatcher.find()){
            //                 String targetCoords = responseMatcher.group(0).strip();
            //                 String[] axisLocations = targetCoords.split(" ");

            //                 //strips out the two letter G1/G0 command.
            //                 String strippedCommand = bufferLine.substring(2, bufferLine.length()).strip();

            //                 String[] gcodeAxisTargets = strippedCommand.split(" ");

            //                 //Going to have to use a doubled for loop for both arrays so we can check all our axis locations between the two.
            //                 //Break out of the inner for loop or something to keep performance good.
            //                 //Switch statement will handle inconsistencies.
            //                 for(int gcodePos = 0; gcodePos < gcodeAxisTargets.length; gcodePos++){
            //                     switch(gcodeAxisTargets[gcodePos].charAt(0)){
            //                         case 'X':
            //                             for(int printArrPos = 0; printArrPos < axisLocations.length; printArrPos++){
            //                                 if(axisLocations[printArrPos].charAt(0) == 'X'){                    //removes the X/Y/Z in the string
            //                                     float targetPos = Float.parseFloat(gcodeAxisTargets[gcodePos].substring(1, gcodeAxisTargets[gcodePos].length()));
            //                                                                                                           //removes the X:/Y:/Z: in the string
            //                                     float actualPos = Float.parseFloat(axisLocations[printArrPos].substring(2, axisLocations[printArrPos].length())); 
            //                                     System.out.println("X target pos: " + targetPos);
            //                                     System.out.println("X actual pos: " + actualPos);
            //                                     if(targetPos != actualPos){
            //                                         xMatch = false;
            //                                     }
                                                
            //                                     break;
            //                                 }
            //                             }
            //                             break;
            //                         case 'Y':
            //                             for(int printArrPos = 0; printArrPos < axisLocations.length; printArrPos++){
            //                                 if(axisLocations[printArrPos].charAt(0) == 'Y'){
            //                                     float targetPos = Float.parseFloat(gcodeAxisTargets[gcodePos].substring(1, gcodeAxisTargets[gcodePos].length()));
            //                                     float actualPos = Float.parseFloat(axisLocations[printArrPos].substring(2, axisLocations[printArrPos].length())); 
            //                                     System.out.println("Y target pos: " + targetPos);
            //                                     System.out.println("Y actual pos: " + actualPos);
            //                                     if(targetPos != actualPos){
            //                                         yMatch = false;
            //                                     }
            //                                     break;
            //                                 }
            //                             }
            //                             break;
            //                         case 'Z':
            //                             for(int printArrPos = 0; printArrPos < axisLocations.length; printArrPos++){
            //                                 if(axisLocations[printArrPos].charAt(0) == 'Z'){
            //                                     float targetPos = Float.parseFloat(gcodeAxisTargets[gcodePos].substring(1, gcodeAxisTargets[gcodePos].length()));
            //                                     float actualPos = Float.parseFloat(axisLocations[printArrPos].substring(2, axisLocations[printArrPos].length())); 
            //                                     System.out.println("Z target pos: " + targetPos);
            //                                     System.out.println("Z actual pos: " + actualPos);
            //                                     if(targetPos != actualPos){
            //                                         zMatch = false;
            //                                     }
            //                                     break;
            //                                 }
            //                             }
            //                             break;
            //                         default:
            //                             System.out.println("default statement hit; " + gcodeAxisTargets[gcodePos]);
            //                             break;
            //                     }
            //                 }
            //                 if(!xMatch || !yMatch || !zMatch){
            //                     System.out.println("error in target head position, resending current command");
            //                     portWriter.write(bufferLine);
            //                     portWriter.newLine();
            //                     portWriter.flush();
            //                 }else{
            //                     okToContinue = true;
            //                 }
            //             }
            //             if(SECONDS.between(currentTime, LocalTime.now()) > 1){
            //                 currentTime = LocalTime.now();
            //                 portWriter.write("M114");// Report position
            //                 portWriter.newLine();
            //                 portWriter.flush();
            //             }
            //         }
            //     }
            // }else{
            //     portWriter.write(bufferLine);
            //     portWriter.newLine();
            //     portWriter.flush();
            //     while(portReader.ready()){
            //         String printerResponse = portReader.readLine().strip();
            //         System.out.println("Printer response: " + printerResponse);
            //         break;
            //     }
            // }

        }
    }

}
