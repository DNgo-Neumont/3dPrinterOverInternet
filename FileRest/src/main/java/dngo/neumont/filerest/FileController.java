package dngo.neumont.filerest;

//import com.hierynomus.smbj.SMBClient;
//import com.hierynomus.smbj.auth.AuthenticationContext;
//import com.hierynomus.smbj.connection.Connection;
import com.azure.storage.common.ParallelTransferOptions;
import com.azure.storage.file.share.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {

    //How to handle SMB3 auth sending
    //Encrypt with RSA private key here, send off and decrypt with public
    //Nevermind just use locally stored SAS keys

    private final String connectUrl = System.getenv("azure_connect_url");
    private final String connectionString = System.getenv("azure_connection_params_string");
    private final String shareName = System.getenv("azure_share_name");
    private final long finalChunkSize = 1048576 * 4; //1st number is raw number of bytes in a megabyte, multiply by second to determine blocksize max


    @RequestMapping(method = RequestMethod.POST, path = "/addFile")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file")MultipartFile file, @RequestParam String username,  RedirectAttributes attributes){
//        SMBClient client = new SMBClient();
//        try(Connection connection = client.connect("simplprintgcode.file.core.windows.net")){
//            AuthenticationContext authContext = new AuthenticationContext("localhost\\simplprintgcode", "qpCNkGEFSRHA6qzpb9Smy19OmYqpCJnk3XRE7ucedjQiZ0Qsn5PZ/V8YUwD1zXY1ASJUkHILBjLP+AStF+8cIQ==".toCharArray(), "gcode-datashare");
//            connection.authenticate(authContext);
//
//            System.out.println(connection.isConnected());
//        }catch(IOException exception){
//            exception.printStackTrace();
//        }
        //Replace with Azure components because of course
        //Seriously though azure is ending up being more secure - just use these.

        System.out.println(connectionString);

        ShareClient azureClient = new ShareClientBuilder().endpoint(connectUrl).connectionString(connectionString).shareName(shareName).buildClient();


        //get user details in json body - create based off of username
        azureClient.createDirectoryIfNotExists(username);

        ShareDirectoryClient directoryClient = azureClient.getDirectoryClient(username);

        directoryClient.createFile(file.getName(), file.getSize());

        ShareFileClient fileClient = azureClient.getFileClient(username + "/" + file.getName());

        byte[] data;

        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long chunkSize = finalChunkSize;

        if(data.length > chunkSize){
            for(int offset = 0; offset < data.length; offset += chunkSize){
                try{
                    chunkSize = Math.min(data.length - offset, chunkSize);

                    byte[] subArray = Arrays.copyOfRange(data, offset, (int) (offset + chunkSize));

                    fileClient.upload(new ByteArrayInputStream(subArray), subArray.length, new ParallelTransferOptions().setBlockSizeLong(finalChunkSize));
                }catch (RuntimeException e){
                    e.printStackTrace();
                    if(fileClient.exists()){
                        fileClient.delete();
                    }
                    throw e;
                }
            }
        }else{
            fileClient.upload(new ByteArrayInputStream(data), data.length, new ParallelTransferOptions().setBlockSizeLong(finalChunkSize));
        }

        Map<String, Object> response = new HashMap<>();

        response.put("message", "file " + file.getName() + "added" );
        response.put("file_path", fileClient.getFilePath());
        response.put("file_url", fileClient.getFileUrl());
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET, path="/getFile/{fileID}")
    public ResponseEntity<Map<String, Object>> getFileById(@PathVariable String fileID){
        return null;
    }

}
