package dngo.neumont.filerest;

//import com.hierynomus.smbj.SMBClient;
//import com.hierynomus.smbj.auth.AuthenticationContext;
//import com.hierynomus.smbj.connection.Connection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {

    //How to handle SMB3 auth sending
    //Encrypt with RSA private key here, send off and decrypt with public

    @RequestMapping(method = RequestMethod.POST, path = "/addFile")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file")MultipartFile file, RedirectAttributes attributes){
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




        return null;
    }

    @RequestMapping(method = RequestMethod.GET, path="/getFile/{fileID}")
    public ResponseEntity<Map<String, Object>> getFileById(@PathVariable String fileID){
        return null;
    }

}
