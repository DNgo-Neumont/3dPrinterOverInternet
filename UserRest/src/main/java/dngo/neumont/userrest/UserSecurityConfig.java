package dngo.neumont.userrest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableWebSecurity
public class UserSecurityConfig {



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.authorizeHttpRequests()
                .antMatchers(HttpMethod.POST, "/user/auth").permitAll()
                .antMatchers(HttpMethod.POST, "/user/piAuth").permitAll()
                .antMatchers(HttpMethod.POST, "/user").permitAll()
                .antMatchers(HttpMethod.GET, "/user/refreshAuth").permitAll()
                .antMatchers(HttpMethod.GET,"/user/**").hasAnyRole("USER","ADMIN")
                .antMatchers(HttpMethod.PUT, "/user/**").hasAnyRole("USER","ADMIN")
                .antMatchers(HttpMethod.DELETE,"/user/**").hasAnyRole("ADMIN")
                .and()
                .addFilterBefore(new RestAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

//Used for BAUTH
//    @Bean
//    public UserDetailsService userDetailsService(){
//        return new MysqlUserDetailsService();
//    }


}
