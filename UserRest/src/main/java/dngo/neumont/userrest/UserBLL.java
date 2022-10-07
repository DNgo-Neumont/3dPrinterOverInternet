package dngo.neumont.userrest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserBLL {

    @Autowired
    UserRepository userRepository;

    public ResponseEntity<Map<String, Object>> getUserById(long userId){
        User foundUser = userRepository.findById(userId).orElse(null);
        Map<String, Object> response = new HashMap<>();

        if(foundUser != null){
            response.put("message", "User found");
            response.put("user",foundUser);
            response.put("timestamp", LocalDateTime.now());

            return new ResponseEntity<>(response, HttpStatus.OK);
        }else{
            response.put("message", "User not found");
            response.put("timestamp", LocalDateTime.now());

            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<Map<String, Object>> addUser(User user){
        User savedUser = userRepository.saveAndFlush(user);

        Map<String, Object> response = new HashMap<>();

        response.put("message", "user saved");
        response.put("user", savedUser);
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    public ResponseEntity<Map<String, Object>> updateUser(){

        return null;
    }

}
