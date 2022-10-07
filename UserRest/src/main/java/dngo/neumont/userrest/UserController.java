package dngo.neumont.userrest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/user")
public class UserController {

    @RequestMapping(method = RequestMethod.GET, path="/{id}")
    public void getUserById(@PathVariable long id){
        //call to bll with ID for getting a user
    }

}
