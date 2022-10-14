package dngo.neumont.userrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class}) //Excludes default autogenerated user
//And only allows for JWT authentication from what I can garner.
public class UserRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserRestApplication.class, args);
    }

}
