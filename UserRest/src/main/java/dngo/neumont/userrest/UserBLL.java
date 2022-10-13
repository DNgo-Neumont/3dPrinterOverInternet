package dngo.neumont.userrest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

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
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));

        User savedUser = userRepository.saveAndFlush(user);

        Map<String, Object> response = new HashMap<>();

        response.put("message", "user saved");
        response.put("user", savedUser);
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    public ResponseEntity<Map<String, Object>> updateUser(long id, JsonNode userDetails){
//        if(userRepository.findById(userDetails.get("id").asLong()).isPresent()){
//            User userToUpdate = userRepository.findById(userDetails.get("id").asLong()).get();
        if(userRepository.findById(id).isPresent()){
            User userToUpdate = userRepository.findById(id).get();
            Iterator<String> keySet = userDetails.fieldNames();

            while(keySet.hasNext()){
                String key = keySet.next();
                switch(key){
                    case "user_name":
                        userToUpdate.setUserName(userDetails.get("user_name").asText());
                        break;
                    case "user_email":
                        userToUpdate.setUserEmail(userDetails.get("user_email").asText());
                        break;
                    case "password":
                        userToUpdate.setPassword(new BCryptPasswordEncoder().encode(userDetails.get("password").asText()));
                    default:
                        break;
                }
            }

            userRepository.saveAndFlush(userToUpdate);

            Map<String, Object> response = new HashMap<>();
            Map<String, Object> userDetailsMap = new HashMap<>();

            userDetailsMap.put("user_name", userToUpdate.getUserName());
            userDetailsMap.put("user_email", userToUpdate.getUserEmail());

            response.put("message", "user " + id + " successfully updated");
            response.put("user-details", userDetailsMap);
            response.put("timestamp", LocalDateTime.now());

            return new ResponseEntity<>(response, HttpStatus.OK);
        }else{
            Map<String, Object> response = new HashMap<>();

            response.put("message", "user with id of " + id + "not found");
            response.put("timestamp", LocalDateTime.now());

            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<Map<String, Object>> deleteUser(long id){
        if(userRepository.findById(id).isPresent()) { // && auth == true)
            User userToDelete = userRepository.findById(id).get();
            userRepository.delete(userToDelete);
            Map<String, Object> response = new HashMap<>();

            response.put("message", "User deleted");
            response.put("user", userToDelete);
            response.put("timestamp", LocalDateTime.now());

            return new ResponseEntity<>(response, HttpStatus.OK);
        }else{
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User not found");
            response.put("timestamp", LocalDateTime.now());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }


    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        User userToCheck = userRepository.findByUserName(username);
        if(userToCheck == null){
            throw new UsernameNotFoundException("User not found");
        }else{
            return userToCheck;
        }

    }
}
