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
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import static java.time.temporal.ChronoUnit.SECONDS;
// import static java.time.temporal.ChronoUnit.MILLIS;


import com.fazecast.jSerialComm.SerialPort;

public class ByteBufferProcessor implements Runnable{


    File gCodeFile;

    BufferedReader gcodeReader;

    BufferedReader portReader;
    
    BufferedWriter portWriter;

    SerialPort printerPort;

    long gcodeLineCount = 0;

    long currentLineNumber = 0;

    List<String> bufferHistory = new ArrayList<>();
    //Buffer and buffer history size, respectively
    int currentBytesSent;
    //Safe bet for most printers
    int maxBytesToSend = 128;

    String waitingLine = "";
    
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
    

    // int historySize = 20;

    @Override
    public void run() {
        try {
            processAndSend();
        } catch (Exception e) {
            System.err.println("Error running gcode for printer: ");
            e.printStackTrace();
        } 
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
    
    public void processAndSend() throws Exception{
        while(currentLineNumber != gcodeLineCount){
            //Create a rabbitMQ producer here and send data back to frontend
            //This class will be wrapped into another rabbitMQ consumer thread that will queue up prints
            //and print them according to whatever it consumes.
            String currentGcodeLine = "";
            
            if(waitingLine.isBlank()){
                
                currentGcodeLine = gcodeReader.readLine();
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
                
                System.out.println("current line: " + currentGcodeLine);
                
                if(currentGcodeLine == null) break;
            }else{
                currentGcodeLine = waitingLine;
            }
            
            
            // if(bufferHistory.size() < historySize){
            //     bufferHistory.add(currentGcodeLine);
            // }else{
                //     bufferHistory.remove(0);
                //     bufferHistory.add(currentGcodeLine);
                // }
                
                if(!((currentBytesSent + currentGcodeLine.getBytes().length) > maxBytesToSend)){
                    
                    
                //     if(currentGcodeLine.contains("M190")){ // bed temp warm command
                //         // System.out.println("Stepped into M190 statement");
                //     handleHeatAndCool(currentGcodeLine, "B");
                //     currentBytesSent += currentGcodeLine.getBytes().length;
                //     bufferHistory.add(currentGcodeLine);
                // }else if(currentGcodeLine.contains("M104") || currentGcodeLine.contains("M109")){ // extruder warm command
                //     // System.out.println("Current gcode line: ");
                //     // System.out.println("Stepped into m104 or m109 statement");
                //     handleHeatAndCool(currentGcodeLine, "T");
                //     currentBytesSent += currentGcodeLine.getBytes().length;
                //     bufferHistory.add(currentGcodeLine);
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
                    currentBytesSent += currentGcodeLine.getBytes().length;
                    bufferHistory.add(currentGcodeLine);
                }else{
                    portWriter.write(currentGcodeLine);
                    portWriter.newLine();
                    portWriter.flush();

                    currentBytesSent += currentGcodeLine.getBytes().length;
                    bufferHistory.add(currentGcodeLine);
                }

                waitingLine = "";
            }else{
                waitingLine = currentGcodeLine;
            }
            

            LocalTime startRead = LocalTime.now();

            while(portReader.ready()){
                // Build a secondary thread that gets the printer responses - better yet, use the JserialComm event listeners to deal with 
                // errors. 
                
                if(SECONDS.between(startRead, LocalTime.now()) > 5){
                    if(bufferHistory.size() > 0){
                        System.out.println("fail safe hit - removing " + bufferHistory.get(0) + " from bytes sent");
                        currentBytesSent -= bufferHistory.remove(0).getBytes().length;
                    }
                    break;
                }

                String response = portReader.readLine();
                System.out.println("Printer response: " + response);

                Pattern coordPattern = Pattern.compile("X:.{0,20} Y:.{0,20} Z:.{0,20} E:.{0,20}", Pattern.DOTALL);

                Matcher matcher = coordPattern.matcher(response.strip()); 
                
                if((response.contains("ok") || matcher.find()) && bufferHistory.size() > 0){
                    currentBytesSent -= bufferHistory.get(0).getBytes().length;
                    System.out.println("Removed " + bufferHistory.remove(0) + " from list");
                }

            }


        }
    }
    
    
}
