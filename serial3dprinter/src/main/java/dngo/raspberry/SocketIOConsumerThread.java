package dngo.raspberry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
// import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
// import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

// import javax.net.ssl.HostnameVerifier;
// import javax.net.ssl.SSLContext;
// import javax.net.ssl.SSLSession;
// import javax.net.ssl.TrustManager;
// import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
// import io.socket.engineio.client.Socket;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
// import okhttp3.OkHttpClient;

public class SocketIOConsumerThread {

    
    // boolean stop = false;

    String username = "";

    List<GcodeProcessor> processorList = new ArrayList<>();

    //Use HTTP for right now to just get a connection together.
    // String socketHost = "https://simplprint3d.com/socket";
    // String socketHost = "https://simplprint3d.com/";
    // String socketHost = "http://localhost:80/socket";
    // Stripping out the /socket because socket.io has been properly mapped now
    // String secondarySocketHost = "https://simplprint.azurewebsites.net/socket";
    String secondarySocketHost = "https://simplprint.azurewebsites.net/";
    // String secondarySocketHost = "http://localhost:80/";
    
    io.socket.client.Socket connectionSocket;
    
    public SocketIOConsumerThread(String token) throws Exception{

        Map<String, String> tokenMap = new HashMap<>();

        tokenMap.put("token", token);

        // HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        //     @Override
        //     public boolean verify(String hostname, SSLSession sslSession) {
        //         if(hostname.contains("simplprint3d.com") || hostname.contains("simplprint")){
        //             return true;
        //         }
        //         else{
        //             return false;
        //         }
        //     }
        // };
        
        // X509TrustManager trustManager = new X509TrustManager() {
        //     public X509Certificate[] getAcceptedIssuers() {
        //         return new X509Certificate[] {};
        //     }
        
        //     @Override
        //     public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
        //         // not implemented
        //     }
        
        //     @Override
        //     public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
        //         // not implemented
        //     }
        // };
        
        // SSLContext sslContext = SSLContext.getInstance("TLS");
        // sslContext.init(null, new TrustManager[] { trustManager }, null);
        
        // OkHttpClient okHttpClient = new OkHttpClient.Builder()
        //         .hostnameVerifier(hostnameVerifier)
        //         .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
        //         .readTimeout(1, TimeUnit.MINUTES) // important for HTTP long-polling
        //         .build();
        
        
                
        //Build out our connection params - auth takes our auth token, secure sets us up to use SSL connections
        IO.Options options = IO.Options.builder().setAuth(tokenMap).setSecure(true).setTimeout(20000).build();
        // options.callFactory = okHttpClient;
        // options.webSocketFactory = okHttpClient;

        try {
            connectionSocket = IO.socket(new URI(secondarySocketHost), options);
            
            connectionSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener(){
                @Override
                public void call(Object... args) {
                    System.out.println(connectionSocket.connected());
                    System.out.println("Socket connected successfully.");
                }
            });
    
            connectionSocket.on("queue", new Emitter.Listener() {
    
                @Override
                public void call(Object... args) {
                    System.out.println(args[0]);
                    // Map<String, Object> testConversion = new HashMap<>(args[0]);
                    JSONObject testConversion = (JSONObject) args[0];
                    
                    System.out.println(testConversion);
                    try {
                        String filename = testConversion.getString("file");
                        String printer = testConversion.getString("printer");
                        URL url = new URL("https://simplprint.azurewebsites.net/file/getFile/" + username + "/" + filename +"/");
                        // URL url = new URL("http://localhost:80/file/getFile/" + username + "/" + filename);
                        
                        URLConnection properConnection = url.openConnection();

                        String bearerString = "Bearer " + token;

                        System.out.println(bearerString);

                        properConnection.setRequestProperty("Authorization", bearerString);
                        ReadableByteChannel channel = Channels.newChannel(properConnection.getInputStream());
                        if(!Files.exists(Path.of("./tempFiles"))){
                            Files.createDirectory(Path.of("./tempFiles"));
                        }
                        if(!Files.exists(Path.of("./tempFiles/" + filename))){
                            Files.createFile(Path.of("./tempFiles/" + filename));
                        }

                        Path localDirectory = Path.of("./tempFiles");
                        Path createdFile = Path.of(localDirectory + "/" + filename);
                        

                        FileOutputStream fileOutputStream = new FileOutputStream(createdFile.toString());
                        // FileChannel fileChannel = fileOutputStream.getChannel();

                        fileOutputStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
                        fileOutputStream.close();

                        boolean printerFound = false;
                        //Iterate through our processor list and queue a file
                        for(GcodeProcessor processor : processorList){
                            if(processor.getDefinedName().toLowerCase().equals(printer.toLowerCase())){
                                processor.setGcodeFile(createdFile.toFile());
                                Thread printerThread = new Thread(processor);
                                printerThread.start();
                                printerFound = true;
                            }
                        }

                        if(!printerFound){
                            System.out.println("No printer was found with that name.");
                            Files.delete(createdFile);
                        }


                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    
                }
            });

            connectionSocket.on("request-update", new Emitter.Listener() {

                @Override
                public void call(Object... args) {

                    List<Map<String, Object>> processorStatuses = new ArrayList<>();

                    // System.out.println(processorList.size());

                    for (GcodeProcessor processor : processorList) {
                        // System.out.println(processor.getDefinedName());
                        // System.out.println(processor.getPortName());
                        // System.out.println(processor.reportStatus());
                        Map<String, Object> processorStatus = new HashMap<>();
                        processorStatus.put("printer-name", processor.getDefinedName());
                        processorStatus.put("printer-progress", processor.reportStatus());
                        processorStatuses.add(processorStatus);
                    }

                    Map<String, Object> response = new HashMap<>();

                    response.put("user", username);
                    response.put("status", processorStatuses);
                    connectionSocket.emit("status", response);
                }
            });


            connectionSocket = connectionSocket.connect();    
            // if(!connectionSocket.connected()){
            //     System.out.println("Stepped into secondary try for connection");
            //     connectionSocket = IO.socket(new URI(secondarySocketHost), options);
            //     connectionSocket.open();
            // }
            // connectionSocket.emit("status", "this is from the java client as a test");
            
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            System.out.println("Malformed URI - failed to create connection!");
            e.printStackTrace();
        }
        
        // if(!connectionSocket.connected()){
        //     throw new Exception("Failed to connect to socket server!");
        // }

    }

    public void setProcessorList(List<GcodeProcessor> processorList){
        this.processorList = processorList;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void requestStop(){
        connectionSocket.close();
        //If we have running processors, request their stops
        for(GcodeProcessor processor: processorList){
            processor.requestStop();
        }

        //Clear out our files
        try {
            Path path = Path.of("./tempFiles");
            if(Files.exists(path)){
                //Pulled from Baeldung: https://www.baeldung.com/java-delete-directory
                Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
            Files.createDirectory(path);
        } catch (IOException e) {
            System.err.println("Failed to clear temporary file storage - please check permissions");
            e.printStackTrace();
        }
        // stop = true;
    }

    // public void spinDown(){
    //     if(stop){
    //     }
    // }

}
