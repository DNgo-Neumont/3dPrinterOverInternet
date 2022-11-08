package dngo.neumont.userrest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;


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

    //Modify to only take in JSON nodes so users can't just add themselves as admins.
    //Build a second endpoint for admin access that adds admins.
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

//        System.out.println(request.getServletPath());
//        System.out.println(request.getContextPath());
//        System.out.println(request.getPathInfo());
//        System.out.println(request.toString());

        User userToCheck = userBLL.loadUserByUsername(userName);

        if(BCrypt.checkpw(password, userToCheck.getPassword())){
            System.out.println("Successfully logged in user: " + userName);

            String rawRoles = userToCheck.getRoles();
            //This whole mess splits the CS values and strips them of whitespace to collect into a list for sending.
            List<String> roles = Arrays.stream(rawRoles.split(",")).map((String s) -> {s = s.strip(); return s;}).collect(Collectors.toList());

            Algorithm algorithm = Algorithm.HMAC256(auth_secret.getBytes());
            String accessToken = JWT.create()
                    .withSubject(userToCheck.getUserName()) //Better way of determining expiry time
                    .withExpiresAt(Instant.from(ZonedDateTime.of(LocalDateTime.now().plusMinutes(10), ZoneId.systemDefault())))
                    .withIssuer(request.getRequestURL().toString())
                    //Change to pulling roles from DB
                    .withClaim("roles", roles)
                    .sign(algorithm);
            String refreshToken = JWT.create()
                    .withSubject(userToCheck.getUserName()) //Better way of determining expiry time
                    .withExpiresAt(Instant.from(ZonedDateTime.of(LocalDateTime.now().plusMinutes(30), ZoneId.systemDefault())))
                    .withIssuer(request.getRequestURL().toString())
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

    @RequestMapping(method = RequestMethod.GET, path="/refreshAuth")
    public ResponseEntity<Map<String, Object>> refreshAuthToken(HttpServletRequest request){
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String refreshToken = authHeader.substring("Bearer ".length());
                Algorithm algorithm = Algorithm.HMAC256(auth_secret.getBytes());
                JWTVerifier jwtVerifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = jwtVerifier.verify(refreshToken);
                String userName = decodedJWT.getSubject();
                List<String> rolesOfJWT = (List<String>) decodedJWT.getClaim("roles");

                if(rolesOfJWT.contains("PI_USER")){
                    throw new IllegalArgumentException("Cannot refresh with tokens meant for the consumer program!");
                }

                User user = userBLL.loadUserByUsername(userName);

                if(user != null){
                    String accessToken = JWT.create()
                            .withSubject(user.getUserName())
                            .withExpiresAt(Instant.from(ZonedDateTime.of(LocalDateTime.now().plusMinutes(10), ZoneId.systemDefault())))
                            .withClaim("roles", List.of("ROLE_USER"))
                            .withIssuer(request.getRequestURL().toString())
                            .sign(algorithm);
//                    refreshToken = JWT.create()
//                            .withSubject(user.getUserName()) //Better way of determining expiry time
//                            .withExpiresAt(Instant.from(ZonedDateTime.of(LocalDateTime.now().plusMinutes(10), ZoneId.systemDefault())))
//                            .withIssuer(request.getContextPath())
//                            .sign(algorithm);
                    Map<String, Object> tokenResponse = new HashMap<>();

                    tokenResponse.put("access_token", accessToken);
                    tokenResponse.put("refresh_token", refreshToken);
                    tokenResponse.put("timestamp", LocalDateTime.now());

                    return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
                }else{
                    //SOMEONE'S BEEN IN HERE
                    //Either the token's been tampered with or we've nuked the user from the database
                    //Return an appropriate response

                    Map<String, Object> errorResponse = new HashMap<>();

                    errorResponse.put("message", "User not found in database");
                    errorResponse.put("timestamp", LocalDateTime.now());
                    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {

                Map<String, Object> exceptionResponse = new HashMap<>();

                e.printStackTrace();

                exceptionResponse.put("message", e.getLocalizedMessage());
                exceptionResponse.put("timestamp", LocalDateTime.now());

                return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }else{

            Map<String, Object> noTokenResponse = new HashMap<>();

            noTokenResponse.put("message", "no token provided");
            noTokenResponse.put("timestamp",  LocalDateTime.now());

            return new ResponseEntity<>(noTokenResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path="/piAuth")
    public ResponseEntity<Map<String, Object>> raspberryPiAuth(HttpServletRequest request, @RequestBody JsonNode userDetails){
        String userName = userDetails.get("user_name").asText();
        String password = userDetails.get("password").asText();

        User userToCheck = userBLL.loadUserByUsername(userName);

        if(BCrypt.checkpw(password, userToCheck.getPassword())){
            System.out.println("Successfully logged in user (PI): " + userName);

            String rawRoles = userToCheck.getRoles();
            //This whole mess splits the CS values and strips them of whitespace to collect into a list for sending.
//            List<String> roles = Arrays.stream(rawRoles.split(",")).map((String s) -> {s = s.strip(); return s;}).collect(Collectors.toList());

            List<String> roles = List.of("ROLE_PI_USER");

            Algorithm algorithm = Algorithm.HMAC256(auth_secret.getBytes());
            String accessToken = JWT.create()
                    .withSubject(userToCheck.getUserName()) //Better way of determining expiry time
                    .withExpiresAt(Instant.from(ZonedDateTime.of(LocalDateTime.now().plusDays(10), ZoneId.systemDefault())))
                    .withIssuer(request.getRequestURL().toString())
                    //Change to pulling roles from DB
                    .withClaim("roles", roles)
                    .sign(algorithm);
            String refreshToken = JWT.create()
                    .withSubject(userToCheck.getUserName()) //Better way of determining expiry time
                    .withExpiresAt(Instant.from(ZonedDateTime.of(LocalDateTime.now().plusDays(30), ZoneId.systemDefault())))
                    .withIssuer(request.getRequestURL().toString())
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
