package dngo.neumont.userrest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
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

    @RequestMapping(method = RequestMethod.POST, path="/auth")
    public ResponseEntity<Map<String, Object>> authenticateUser(@RequestBody JsonNode userDetails){
        String userName = userDetails.get("user_name").asText();
        String password = userDetails.get("password").asText();



        Map<String, Object> response = new HashMap<>();


        response.put("User details", userBLL.loadUserByUsername(userName));
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
