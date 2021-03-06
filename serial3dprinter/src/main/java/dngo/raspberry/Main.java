package dngo.raspberry;

import java.io.BufferedReader;
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
            }catch(Exception e){
                e.printStackTrace();
            }

            //TODO
            //ADD a data listener class that queues up gcode and reports current printer echobacks to console
            //Work in a rabbitMQ message consumer that polls this classes' current progress or just hits this in a springboot container
            //Figure out how to use slic3r from the java runtime and have it slice raw gcode OR - just have customers send in raw gcode to the specifications of currently connected printers.
            //may be worth it to grab the params returned by M115
            //the majority of the program is built there, I might need to look into threading for adding multiple printers.

        }
        
    }
}
