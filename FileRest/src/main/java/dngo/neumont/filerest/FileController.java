package dngo.neumont.filerest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {

    @RequestMapping(method = RequestMethod.GET, path="/getFile/{fileID}")
    public ResponseEntity<Map<String, Object>> getFileById(@PathVariable String fileID){
        return null;
    }

}
