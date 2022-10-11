package dngo.neumont.userrest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createNewUser(@RequestBody User user){
        return userBLL.addUser(user);
    }

    @RequestMapping(method = RequestMethod.PUT, path="/{id}")
    public ResponseEntity<Map<String, Object>> updateUserById(@PathVariable long id, @RequestBody JsonNode userDetails){
        return userBLL.updateUser(id, userDetails);
    }

    @RequestMapping(method = RequestMethod.DELETE, path="/{id}")
    public ResponseEntity<Map<String, Object>> deleteUserById(@PathVariable long id){
        //call to bll with ID> for getting a user
        return userBLL.deleteUser(id);
    }

}
