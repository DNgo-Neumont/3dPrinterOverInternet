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

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class PrinterSerialController implements SerialPortDataListener {

    int linesInPrinterBuffer = 0;
    int artificialPrinterBuffer = 7;
    File workingGcodeFile;
    SerialPort printerPort;
    BufferedReader gCodeReader;
    BufferedReader serialPortReader;
    BufferedWriter serialPortWriter;
    Boolean gCodeFileStillProcessing = true;

    public void setGcodeFile(File file){
        workingGcodeFile = file;
        try {
            gCodeReader = new BufferedReader(new FileReader(workingGcodeFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setPort(SerialPort port){
        printerPort = port;
        serialPortReader = new BufferedReader(new InputStreamReader(port.getInputStream()));
        serialPortWriter = new BufferedWriter(new OutputStreamWriter(port.getOutputStream()));
    }

    public void processGcodeFile() throws IOException{
        long totalGcodeLines = Files.lines(workingGcodeFile.toPath()).count();
        do {
            long processedLineCount = 0;

            if (linesInPrinterBuffer <= artificialPrinterBuffer) {
                String currentGcodeCommand = gCodeReader.readLine();
                
                if (!(currentGcodeCommand.charAt(0) == ';' || currentGcodeCommand.isBlank())) {
                    serialPortWriter.write(currentGcodeCommand);
                    serialPortWriter.newLine();
                    serialPortWriter.flush();
                    linesInPrinterBuffer++;
                    processedLineCount++;
                    System.out.println("LOG: Sent printer command: " + currentGcodeCommand);
                } else if (currentGcodeCommand.charAt(0) == ';') {
                    System.out.println("LOG: Skipped gcode comment.");
                    processedLineCount++;
                } else if (currentGcodeCommand.isBlank()) {
                    System.out.println("LOG: Skipped blank line.");
                    processedLineCount++;
                }

            } else if (linesInPrinterBuffer <= 8) {
                System.out.println("LOG: Buffer at configured capacity; skipped sending gcode command.");
            }
            
            float processedPercentage = (float)(Math.round(((processedLineCount/totalGcodeLines) * 100.0)) / 100.0);
            System.out.println("LOG: Processed Lines: " + processedLineCount + "/" + totalGcodeLines + " | " + processedPercentage + "%");
            if (processedLineCount == totalGcodeLines) {gCodeFileStillProcessing = false;}
        } while (gCodeFileStillProcessing == true);
        System.out.println("Total gcode lines: " + totalGcodeLines);
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        String printerResponse;
        try {
            printerResponse = serialPortReader.readLine().strip();
            System.out.println("PMSG: " + printerResponse);
            if (printerResponse.contentEquals("ok")) {
                linesInPrinterBuffer--;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}