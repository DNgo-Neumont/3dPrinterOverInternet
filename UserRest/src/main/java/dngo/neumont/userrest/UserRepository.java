package dngo.neumont.userrest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    //Do this to avoid issues with underscored column names
    public User findByUserName(String user_name);

}
