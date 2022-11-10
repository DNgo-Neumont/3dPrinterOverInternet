package dngo.raspberry;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
// import io.socket.engineio.client.Socket;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

public class SocketIOConsumerThread {

    



    //Use HTTP for right now to just get a connection together.
    // String socketHost = "https://simplprint3d.com/socket";
    String socketHost = "https://simplprint3d.com/";
    // String socketHost = "http://localhost:80/socket";
    // Stripping out the /socket because socket.io has been properly mapped now
    // String secondarySocketHost = "https://simplprint.azurewebsites.net/socket";
    String secondarySocketHost = "https://simplprint.azurewebsites.net/";
    
    io.socket.client.Socket connectionSocket;
    
    public SocketIOConsumerThread(String token) throws Exception{

        Map<String, String> tokenMap = new HashMap<>();

        tokenMap.put("token", token);

        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslSession) {
                if(hostname.contains("simplprint3d.com") || hostname.contains("simplprint")){
                    return true;
                }
                else{
                    return false;
                }
            }
        };
        
        X509TrustManager trustManager = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }
        
            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
                // not implemented
            }
        
            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
                // not implemented
            }
        };
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] { trustManager }, null);
        
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .hostnameVerifier(hostnameVerifier)
                .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                .readTimeout(1, TimeUnit.MINUTES) // important for HTTP long-polling
                .build();
        
        
                
                //Build out our connection params - auth takes our auth token, secure sets us up to use SSL connections
        IO.Options options = IO.Options.builder().setAuth(tokenMap).setSecure(true).setTimeout(20000).build();
        options.callFactory = okHttpClient;
        options.webSocketFactory = okHttpClient;

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
                    
                }
                
            });
            connectionSocket = connectionSocket.connect();    
            // if(!connectionSocket.connected()){
            //     System.out.println("Stepped into secondary try for connection");
            //     connectionSocket = IO.socket(new URI(secondarySocketHost), options);
            //     connectionSocket.open();
            // }
            connectionSocket.emit("status", "this is from the java client as a test");
            
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            System.out.println("Malformed URI - failed to create connection!");
            e.printStackTrace();
        }
        
        // if(!connectionSocket.connected()){
        //     throw new Exception("Failed to connect to socket server!");
        // }



        
    }

}
