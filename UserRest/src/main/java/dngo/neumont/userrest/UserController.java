package dngo.neumont.userrest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path="/user")
public class UserController {

    @Autowired
    private UserBLL userBLL;

    @RequestMapping(method = RequestMethod.GET, path="/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable long id){
        //call to bll with ID> for getting a user
        return userBLL.getUserById(id);
    }

}
