package dngo.neumont.userrest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping(path="/user")
public class UserController {

    private final String auth_secret = System.getenv("auth_secret");
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

// Come back to this for JWT auth - right now testing out basic auth
    @RequestMapping(method = RequestMethod.POST, path="/auth")
    public ResponseEntity<Map<String, Object>> authenticateUser(HttpServletRequest request, @RequestBody JsonNode userDetails){
        String userName = userDetails.get("user_name").asText();
        String password = userDetails.get("password").asText();

        User userToCheck = userBLL.loadUserByUsername(userName);

        if(BCrypt.checkpw(password, userToCheck.getPassword())){
            System.out.println("Successfully logged in user: " + userName);


            Algorithm algorithm = Algorithm.HMAC256(auth_secret.getBytes());
            String accessToken = JWT.create()
                    .withSubject(userToCheck.getUserName())         //Roughly ten mins in millisecs
                    .withExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                    .withIssuer(request.getContextPath())
                    //Change to pulling roles from DB
                    .withClaim("roles", List.of("ROLE_USER"))
                    .sign(algorithm);
            String refreshToken = JWT.create()
                    .withSubject(userToCheck.getUserName())         //Roughly ten mins in millisecs
                    .withExpiresAt(new Date(System.currentTimeMillis() + 30 * 60 * 1000))
                    .withIssuer(request.getContextPath())
                    .sign(algorithm);
            Map<String, Object> response = new HashMap<>();

            response.put("access_token", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("timestamp", LocalDateTime.now());
            return new ResponseEntity<>(response, HttpStatus.OK);
        }else{
            Map<String, Object> response = new HashMap<>();

            response.put("message", "Invalid credentials");
            response.put("timestamp", LocalDateTime.now());
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
    }

}
