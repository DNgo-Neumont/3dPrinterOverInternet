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

            //NOTE: does not follow DRY philosophy but it works so
            //Will clean up later and extract into a seperate method
            if(currentGcodeLine.contains("M190")){ // bed temp warm command
                portWriter.write(currentGcodeLine);
                portWriter.newLine();
                portWriter.flush();
                boolean bedWarm = false;
                // Get a timestamp of the current time for breaking out of the loop. 
                LocalTime currentTime = LocalTime.now();
                while(!bedWarm){
                    String printerResponse = "";
                    if(portReader.ready()){
                        printerResponse = portReader.readLine().strip();
                        System.out.println("Printer response: " + printerResponse);
                    }
                    Pattern bedTempResponsePattern = Pattern.compile("(B:\\d{1,10}\\.?\\d{1,10} /\\d{1,10}\\.?\\d{1,10})", Pattern.MULTILINE);
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

            }else if(currentGcodeLine.contains("M104")){ // extruder warm command
                portWriter.write(currentGcodeLine);
                portWriter.newLine();
                portWriter.flush();
                boolean extruderWarm = false;
                // Get a timestamp of the current time for breaking out of the loop. 
                LocalTime currentTime = LocalTime.now();
                while(!extruderWarm){
                    String printerResponse = "";
                    if(portReader.ready()){
                        printerResponse = portReader.readLine().strip();
                        System.out.println("Printer response: " + printerResponse);
                    }
                    Pattern extruderTempResponsePattern = Pattern.compile("(T:\\d{1,10}\\.?\\d{1,10} /\\d{1,10}\\.?\\d{1,10})");
                    Matcher extruderTempMatcher = extruderTempResponsePattern.matcher(printerResponse);

                    if(extruderTempMatcher.find()){
                        String currentTempString = extruderTempMatcher.group(0);
                        String[] splitString = currentTempString.split(" ");

                        float currentTempFloat = Float.parseFloat(splitString[0].substring(2, splitString[0].length()));//for excluding T:
                        float desiredTemp = Float.parseFloat(splitString[1].substring(1, splitString[1].length()));//for excluding /


                        if(currentTempFloat >= desiredTemp - 1.00 && currentTempFloat < desiredTemp + 1.00){
                            extruderWarm = true;
                        }

                    }else if(SECONDS.between(currentTime, LocalTime.now()) > 3){
                        currentTime = LocalTime.now();
                        portWriter.write("M105");// Report temperatures
                        portWriter.newLine();
                        portWriter.flush();
                        //While loop will handle the rest if this is a preheated extruder.
                    }
                }

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
