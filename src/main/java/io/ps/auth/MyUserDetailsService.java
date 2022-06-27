package io.ps.auth;

import io.ps.auth.model.InstaUserDetails;

import io.ps.auth.model.User;
import io.ps.auth.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    UserService userService;

    @Override
    public InstaUserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
       /* return new User("foo", "foo",
                new ArrayList<>());*/

       /*String username = userService.findByUserName(s).getUsername();
        String password = userService.findByUserName(s).getPassword();
       return new User(username, password,
                new ArrayList<>());*/

        User user = userService.findByUserName(s);
        log.info("User: ", user.getUsername());

        return new InstaUserDetails(user);
    }
}