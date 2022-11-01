package dngo.neumont.filerest;

//import com.hierynomus.smbj.SMBClient;
//import com.hierynomus.smbj.auth.AuthenticationContext;
//import com.hierynomus.smbj.connection.Connection;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.common.ParallelTransferOptions;
import com.azure.storage.file.share.*;

import com.azure.storage.file.share.models.ShareFileItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/file")
public class FileController {

    //How to handle SMB3 auth sending
    //Encrypt with RSA private key here, send off and decrypt with public
    //Nevermind just use locally stored SAS keys

    private final String connectUrl = System.getenv("azure_connect_url");
    private final String connectionString = System.getenv("azure_connection_params_string");
    private final String shareName = System.getenv("azure_share_name");
//    private final long finalChunkSize = 1048576 * 4; //1st number is raw number of bytes in a megabyte, multiply by second to determine blocksize max
    private final String auth_secret = System.getenv("auth_secret");

    Logger logger = LoggerFactory.getLogger(FileController.class);

    @RequestMapping(method = RequestMethod.POST, path = "/addFile")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file")MultipartFile file, @RequestParam String username){
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
//        System.out.println(connectionString);
        ShareClient azureClient = new ShareClientBuilder().endpoint(connectUrl).connectionString(connectionString).shareName(shareName).buildClient();

        //get user details in json body - create based off of username
        azureClient.createDirectoryIfNotExists(username);
        ShareDirectoryClient directoryClient = azureClient.getDirectoryClient(username);
        directoryClient.createFile(file.getOriginalFilename(), file.getSize());
        ShareFileClient fileClient = azureClient.getFileClient(username + "/" + file.getOriginalFilename());

        byte[] data;

        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        long chunkSize = finalChunkSize;
//
//        if(data.length > chunkSize){
//            for(int offset = 0; offset < data.length; offset += chunkSize){
//                try{
//                    chunkSize = Math.min(data.length - offset, chunkSize);
//
//                    byte[] subArray = Arrays.copyOfRange(data, offset, (int) (offset + chunkSize));
//                    System.out.println(new String(subArray));
//                    ByteArrayInputStream inputStream = new ByteArrayInputStream(subArray);
//
//                    fileClient.upload(inputStream, subArray.length, new ParallelTransferOptions());
//                    inputStream.close();
//                }catch (RuntimeException e){
//                    e.printStackTrace();
//                    if(fileClient.exists()){
//                        fileClient.delete();
//                    }
//                    throw e;
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }else{
        //Never use EXPLETIVE microsoft example code again.
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        fileClient.upload(inputStream, data.length, new ParallelTransferOptions());
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        }

        Map<String, Object> response = new HashMap<>();

        response.put("message", "file " + file.getOriginalFilename() + " added" );
        response.put("file_path", fileClient.getFilePath());
        response.put("file_url", fileClient.getFileUrl());
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET, path="/getFile/{username}/{filename}")
    public FileSystemResource getFileById(@PathVariable String username, @PathVariable String filename, HttpServletRequest request, HttpServletResponse response){

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        //Will have to decrypt incoming token later - RSA is likely due to public and private keys
        //Also doesn't need a try catch - only way we get in here is with the token already being valid
        Map<String, Object> tokenMap = verifyToken(authHeader);

        String jwtUsername = (String) tokenMap.get("username");
        List<String> roles = (List<String>) tokenMap.get("roles");

        //Secure with JWT token check for either the username matching or the admin role.
        if(jwtUsername.contentEquals(username) || roles.stream().anyMatch(s -> s.contentEquals("ROLE_ADMIN"))){

            ShareClient client = new ShareClientBuilder().endpoint(connectUrl).connectionString(connectionString).shareName(shareName).buildClient();
            ShareDirectoryClient directoryClient = client.getDirectoryClient(username);
            ShareFileClient fileClient = directoryClient.getFileClient(filename);

            Path tempFile;

            try {
                Path path = Path.of("./tempDir");
                if(Files.exists(path)){
                    //Pulled from Baeldung: https://www.baeldung.com/java-delete-directory
                    Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
                Files.createDirectory(path);

                Path filePath = Path.of("./tempDir/" + filename);
                tempFile = Files.createFile(filePath);


            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Failed to write file " + filename + "! Stacktrace provided");
                throw new RuntimeException(e);
            }

            File toDownload = tempFile.toFile();

            FileOutputStream writer;

            try {
                 writer = new FileOutputStream(toDownload);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Failed to create file writer for file " + filename + "! Stacktrace provided");
                throw new RuntimeException(e);
            }

            fileClient.download(writer);

            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("ok this is getting ridiculous - file output stream could not be closed, stacktrace provided");
                throw new RuntimeException(e);
            }

            File fileToGet = tempFile.toFile();

//            Map<String, Object> response = new HashMap<>();
//
//            response.put("message", "file retrieved");
//            response.put("file", new FileSystemResource(fileToGet));
//            response.put("timestamp", LocalDateTime.now());

//            return new ResponseEntity<>(response, HttpStatus.OK);

            return new FileSystemResource(fileToGet);

        }else {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(APPLICATION_JSON_VALUE);
            Map<String, Object> jsonResponse = new HashMap<>();

            jsonResponse.put("message", "unauthorized");
//            jsonResponse.put("timestamp", LocalDateTime.now());
            try {
                new ObjectMapper().writeValue(response.getOutputStream(),jsonResponse);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return null;
        }


    }

    @RequestMapping(method=RequestMethod.GET, path="/getFiles/{username}")
    public ResponseEntity<Map<String, Object>> getFilesForUser(@PathVariable String username, HttpServletRequest request){

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        Map<String, Object> tokenMap = verifyToken(authHeader);

        String jwtUsername = (String) tokenMap.get("username");
        List<String> roles = (List<String>) tokenMap.get("roles");

        if(jwtUsername.contentEquals(username) || roles.stream().anyMatch(s -> s.contentEquals("ROLE_ADMIN"))){

            ShareClient client = new ShareClientBuilder().endpoint(connectUrl).connectionString(connectionString).shareName(shareName).buildClient();

            ShareDirectoryClient directoryClient = client.getDirectoryClient(username);
            PagedIterable<ShareFileItem> directoryList = directoryClient.listFilesAndDirectories();
            List<String> fileNames = new ArrayList<>();

            for(ShareFileItem item: directoryList){
    //            System.out.println(item.getName());
                fileNames.add(item.getName());
            }

            Map<String, Object> response = new HashMap<>();

            response.put("message", "files for user " + username);
            response.put("files", fileNames);
            response.put("timestamp", LocalDateTime.now());

            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        Map<String, Object> response = new HashMap<>();

        response.put("message", "unauthorized");
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @RequestMapping(method=RequestMethod.DELETE, path="/deleteFile/{username}/{filename}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String username, @PathVariable String filename, HttpServletRequest request){
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        Map<String, Object> tokenMap = verifyToken(authHeader);

        String jwtUsername = (String) tokenMap.get("username");
        List<String> roles = (List<String>) tokenMap.get("roles");

        if(jwtUsername.contentEquals(username) || roles.stream().anyMatch(s -> s.contentEquals("ROLE_ADMIN"))){
            ShareClient client = new ShareClientBuilder().endpoint(connectUrl).connectionString(connectionString).shareName(shareName).buildClient();
            ShareDirectoryClient directoryClient = client.getDirectoryClient(username);
            ShareFileClient fileClient = directoryClient.getFileClient(filename);

            if(fileClient.deleteIfExists()){
                Map<String, Object> response = new HashMap<>();
                response.put("message", "file " + filename + " deleted");
                response.put("timestamp", LocalDateTime.now());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }else{
                Map<String, Object> response = new HashMap<>();
                response.put("message", "file " + filename + " deleted");
                response.put("timestamp", LocalDateTime.now());
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "unauthorized");
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    private Map<String, Object> verifyToken(String authHeader){

        String token = authHeader.substring("Bearer ".length());
        //encryption algo
        Algorithm algorithm = Algorithm.HMAC256(auth_secret.getBytes());
        //JWT verifier to decrypt and use the JWT
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        DecodedJWT decodedJWT = jwtVerifier.verify(token);
        String jwtUsername = decodedJWT.getSubject();
        List<String> roles = decodedJWT.getClaim("roles").asList(String.class);

        Map<String, Object> mapToReturn = new HashMap<>();

        mapToReturn.put("username", jwtUsername);
        mapToReturn.put("roles", roles);

        return mapToReturn;
    }


}


