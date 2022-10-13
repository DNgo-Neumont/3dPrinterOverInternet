package dngo.neumont.userrest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;

public class MysqlUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User userToFind = repository.findByUserName(username);

        if(userToFind == null){
            throw new UsernameNotFoundException("User not found in database");
        }else{
            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            //To be changed out for proper checking from DB columns
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            return new org.springframework.security.core.userdetails.User(userToFind.getUserName(), userToFind.getPassword(), authorities);
        }

    }
}
