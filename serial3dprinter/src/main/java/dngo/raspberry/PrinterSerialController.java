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
import java.text.DecimalFormat;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class PrinterSerialController implements SerialPortDataListener {
    
    long processedLineCount = 0;
    int linesInPrinterBuffer = 0;
    int artificialPrinterBuffer = 1;
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

    public void processGcodeFile() throws IOException, InterruptedException{
        long totalGcodeLines = Files.lines(workingGcodeFile.toPath()).count();
        boolean bufferFullMessageSent = false;
        do {

            if (linesInPrinterBuffer <= artificialPrinterBuffer) {
                String currentGcodeCommand = gCodeReader.readLine();
                bufferFullMessageSent = false;
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
                if (!bufferFullMessageSent) {
                    System.out.println("LOG: Buffer at configured capacity; skipped sending gcode command.");
                    bufferFullMessageSent = true;
                }
                Thread.sleep(1000);
            }
            
            float processedPercentage = (float)((processedLineCount/totalGcodeLines) * 100.0);
            DecimalFormat percentage = new DecimalFormat("###.00");
            System.out.println("LOG: Processed Lines: " + processedLineCount + "/" + totalGcodeLines + " | " + percentage.format(processedPercentage) + "%");
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