package dngo.raspberry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
// import java.util.HashMap;
import java.util.List;
// import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import com.fazecast.jSerialComm.*;

public class Main {

    public static final String exchange_name = "command-exchange";
    public static void main(String[] args){
        List<GcodeProcessor> processorList = new ArrayList<GcodeProcessor>();
        // List<ByteBufferProcessor> processorList = new ArrayList<>();
        //Simple intro
        String ver = "1.0";
        String author = "David V. Ngo";
        System.out.println("Java 3D Printer Interface - ver. " + ver);
        System.out.println("Written by " + author);
        System.out.println("!PLEASE FIND YOUR PRINTER'S SPECIFIED BAUD RATE BEFORE RUNNING THIS PROGRAM!");
        System.out.println("The printer port will most likely be a USB Serial Device or named after the printer itself.");
        





        // RabbitMQConsumerThread rabbitConsumer = new RabbitMQConsumerThread();
        // No longer using rabbitMQ - current azure hosting scheme makes it impractical




        StringBuilder menu = new StringBuilder();

        menu.append("Menu: ").append("\n")
        .append("1. Select new printer").append("\n")
        .append("2. Run gcode on selected printers").append("\n")
        .append("3. Sign in and connect to services ").append("\n")
        .append("4. Exit");

        System.out.println(menu.toString());

        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        
        String menuChoice = "";
        boolean exit = false;
        while (!exit){
            try {
                menuChoice = userInput.readLine();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            switch(menuChoice){
                case "1":
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
            
                            System.out.println("Giving the printer a few seconds to initialize the connection.");
                            Thread.sleep(5000);
                            System.out.println("Attempting to read from the port now...");
            
                            BufferedWriter portWriter = new BufferedWriter(new OutputStreamWriter(portSelected.getOutputStream()));
            
                            portWriter.write("M114");
                            portWriter.newLine();
                            portWriter.flush();
                            Thread.sleep(2000);
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
                                        
                                        break;
                                    case "n":
                                        correctPrinter = true;
                                        break;
                                    default:
                                        System.out.println("Please enter only the characters y/n: ");
                                        break;
                                    }
                            }
                            

                            System.out.println("Enter a more readable name for this printer");
                            String printerName = userInput.readLine();
                            

                            fault = true;
                            GcodeProcessor gcodeProcessor = new GcodeProcessor();
                            gcodeProcessor.setPort(portSelected);
                            gcodeProcessor.setDefinedName(printerName);

                            // ByteBufferProcessor gcodeProcessor = new ByteBufferProcessor();
                            // gcodeProcessor.setPort(portSelected);

                            processorList.add(gcodeProcessor);
    
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    break;
                    case "2":
    
                        System.out.println("Select the printer you want to use.");
    
                        for(int i = 0; i< processorList.size(); i++){
                            System.out.println((i+ 1) + ". " + processorList.get(i).getPortName());
                        }
                        
                        boolean correctSelection = false;
    
                        // ByteBufferProcessor processor = null;
                        GcodeProcessor processor = null;
                        int selectedPrinter = 0;
                        while(!correctSelection){
                            
                            try {
                                selectedPrinter = Integer.parseInt(userInput.readLine()) - 1;
                                processor = processorList.get(selectedPrinter);
                                correctSelection = true;
                            } catch (NumberFormatException | IOException e) {
                                // TODO Auto-generated catch block
                                correctSelection = false;
                                e.printStackTrace();
                            } catch (IndexOutOfBoundsException e) {
                                correctSelection = false;
                                e.printStackTrace();
                            }
    
                        }  
                        
                        System.out.println("Select the gcode file you want to use: ");
                        
                        File rootPath = new File("./");
                        
                        File[] fileList = rootPath.listFiles();
                        for(int i = 0; i < fileList.length; i++){
                            System.out.println((i + 1) + ". " + fileList[i].getName());
                        }
                
                        System.out.print("Enter your choice now: ");
                
    
                        correctSelection = false;
                        int selectedFile = 0;
                        File file = null;
                        while(!correctSelection){
                            
                            try {
                                selectedFile = Integer.parseInt(userInput.readLine()) - 1;
                                file = fileList[selectedFile];
                                processor.setGcodeFile(file);
                                correctSelection = true;
                            } catch (NumberFormatException | IOException e) {
                                // TODO Auto-generated catch block
                                correctSelection = false;
                                e.printStackTrace();
                            } catch(IndexOutOfBoundsException e){
                                correctSelection = false;
                                e.printStackTrace();
                            }
                        }
                        
                        Thread printerThread = new Thread(processor);
                        printerThread.start();
                    break;
                case "3":

                    try {
                        System.out.println("Enter your username: ");
                        String username = userInput.readLine();
                        System.out.println("Enter your password: ");
                        String password = new String(System.console().readPassword());//userInput.readLine();

                        String url = "https://simplprint3d.com/user/piAuth";

                        URL authUrl = new URL(url);

                        HttpsURLConnection connection = (HttpsURLConnection) authUrl.openConnection();
                        connection.setRequestMethod("POST");
                        
                        // Map<String, String> requestBody = new HashMap<>();
                        // requestBody.put("user_name", username);
                        // requestBody.put("password", password);
                        
                        String requestBody = "{\"user_name\" : \""+ username + "\", \"password\":\"" + password + "\" }";
                    
                        // System.out.println(requestBody);

                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setRequestProperty("Content-length", String.valueOf(requestBody.toString().length()));
                        connection.setDoOutput(true);
                        connection.setDoInput(true);

                        DataOutputStream httpsStream = new DataOutputStream(connection.getOutputStream());
                        httpsStream.writeBytes(requestBody.toString());
                        httpsStream.close();

                        System.out.println("Response message: " + connection.getResponseMessage());

                        DataInputStream readStream = new DataInputStream(connection.getInputStream());

                        String result = new String(readStream.readAllBytes());

                        System.out.println("Response: " + new JSONObject(result));

                        SocketIOConsumerThread socketTest = new SocketIOConsumerThread(new JSONObject(result).get("access_token").toString());

                        socketTest.setProcessorList(processorList);
                        socketTest.setUsername(username);

                    
                    } catch (IOException | JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    break;

                case "4":
                    exit = true;
                    break;
                default:
                    System.out.println("Please reenter your command - just a number from the ranges given for the menu.");
                    System.out.println(menu);
                    break;
            }

            // clrscr();
            System.out.println(menu);
        }




                // yesNo = "";

                // GcodeListener fileConsumer = new GcodeListener();

                // PrinterSerialController controller = new PrinterSerialController();


                // fileConsumer.setGcodeFile(file);
                // portSelected.addDataListener(fileConsumer);
                // controller.setGcodeFile(file);
                // controller.setPort(portSelected);
                // portSelected.addDataListener(controller);
                // controller.processGcodeFile();

                // fileConsumer.setPort(portSelected);
                // fileConsumer.sendFirst();
                // while(!fileConsumer.finishedPrinting());

                
                // gcodeProcessor.setGcodeFile(file);
                // gcodeProcessor.processAndSend();


            //TODO
            //ADD a data listener class that queues up gcode and reports current printer echobacks to console - IN PROGRESS/No longer viable
            //Multithreading and daemon shenanigans - OTW
            //Figure out a safer way to keep the program running or have the printer initialization code run on a port being connected - In progress
            //Work in a rabbitMQ message consumer that polls this classes' current progress or just hits this in a springboot container - OTW
            //Figure out how to use slic3r from the java runtime and have it slice raw gcode OR - just have customers send in raw gcode to the specifications of currently connected printers. - No slicing - sending OTW
            //may be worth it to grab the params returned by M115 
            //the majority of the program is built there, I might need to look into threading for adding multiple printers.
            //EDIT:  a month into the future and i've got so much to extend and fix

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

    //Pulled from https://stackoverflow.com/questions/2979383/how-to-clear-the-console 
    //With the else statement from https://stackoverflow.com/questions/10241217/how-to-clear-console-in-java
    public static void clrscr(){
        //Clears Screen in java
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033[H\033[2J");
                System.out.flush();
        } catch (IOException | InterruptedException ex) {}
    }
        
}

