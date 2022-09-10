package dngo.raspberry;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import com.fazecast.jSerialComm.*;

public class Main {
    public static void main(String[] args){
        //Simple intro
        String ver = "1.0";
        String author = "David V. Ngo";
        System.out.println("Java 3D Printer Interface - ver. " + ver);
        System.out.println("Written by " + author);
        System.out.println("!PLEASE FIND YOUR PRINTER'S SPECIFIED BAUD RATE BEFORE RUNNING THIS PROGRAM!");
        System.out.println("The printer port will most likely be a USB Serial Device or named after the printer itself.");
        
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        
        SerialPort[] ports = SerialPort.getCommPorts();
        SerialPort portSelected = null;
        
        boolean fault = false;
        while(!fault){
            try{
                System.out.println("Select your printer from the list of ports below.");
                for(int i = 0; i< ports.length; i++){
                    System.out.println( i + 1 + ". " + ports[i].getDescriptivePortName());
                }
                int result = Integer.valueOf(userInput.readLine()) - 1;

                portSelected = ports[result];
                
                System.out.println("Port selected - enter the baud rate for the port now.");

                int baudRate = Integer.valueOf(userInput.readLine());

                portSelected.setBaudRate(baudRate);
                portSelected.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
                portSelected.openPort();

                System.out.println("Giving the printer a second to initialize the connection.");
                Thread.sleep(1000);
                System.out.println("Attempting to read from the port now...");

                BufferedReader portReader = new BufferedReader(new InputStreamReader(portSelected.getInputStream()));
                while(portReader.ready()){
                    System.out.println(portReader.readLine());
                }

                String yesNo = "";  
                boolean correctPrinter = false;
                while(!correctPrinter){
                    yesNo = "";
                    System.out.println("Is the port selected the printer you wanted? Y/N");
                    yesNo = userInput.readLine();
                    yesNo = yesNo.toLowerCase();
                    switch(yesNo){
                        case "y":
                            correctPrinter = true;
                            fault = true;
                            
                            break;
                        case "n":
                            correctPrinter = true;
                            break;
                        default:
                            System.out.println("Please enter only the characters y/n: ");
                            break;
                    }
                }

                yesNo = "";

                GcodeListener fileConsumer = new GcodeListener();

                File file = new File("./test3dPrint.gcode");
                
                fileConsumer.setGcodeFile(file);

                fileConsumer.setPort(portSelected);

                fileConsumer.sendFirst();

                portSelected.addDataListener(fileConsumer);

                while(!fileConsumer.finishedPrinting());

            }catch(Exception e){
                e.printStackTrace();
            }

            //TODO
            //ADD a data listener class that queues up gcode and reports current printer echobacks to console - IN PROGRESS
            //Multithreading and daemon shenanigans
            //Figure out a safer way to keep the program running or have the printer initialization code run on a port being connected
            //Work in a rabbitMQ message consumer that polls this classes' current progress or just hits this in a springboot container
            //Figure out how to use slic3r from the java runtime and have it slice raw gcode OR - just have customers send in raw gcode to the specifications of currently connected printers.
            //may be worth it to grab the params returned by M115
            //the majority of the program is built there, I might need to look into threading for adding multiple printers.

            //Multithreading behavior target
            //Multiple printers connected to a pi
            //all are stored in a ready-to-print list
            //When starting a thread a printer will be selected based on params
            //Once started the thread will remove the printer from the list
            //The pi will continue to run the main program loop waiting on 
            //incoming gcode and will pop printers off the list as fit
            //Once threads finish executing the printer will be made available again
            //and put back into the ready list
            //Gotta make a listener to listen for the add function
            //(or maybe just clever writing)

        }
        
    }
}
