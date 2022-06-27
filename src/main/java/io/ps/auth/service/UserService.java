package io.ps.auth.service;

import io.ps.auth.exception.EmailAlreadyExistsException;
import io.ps.auth.exception.UserNameAlreadyExistsException;
import io.ps.auth.messaging.UserEventSender;
import io.ps.auth.model.Role;
import io.ps.auth.model.User;
import io.ps.auth.repository.UserRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserEventSender userEventSender;


    public List<User> findAll(){
        log.info("Retrieving all users");
        return userRepository.findAll();
    }

    @SneakyThrows
    public User findByUserName(String username){
        log.info("Retrieve user by username");
       return userRepository.findByUsername(username)
               .orElseThrow(() -> new ResourceNotFoundException(username));
    }

    public List<User> findByUserNameIn(List<String> usernames){
        log.info("Retrieve users by usernames");
        return userRepository.findByUsernameIn(usernames);
    }

    public User registerUser(User user){
            log.info("Register a new user");

        if(userRepository.existsByUsername(user.getUsername())){
            log.warn("username {} already present", user.getUsername());
            throw new UserNameAlreadyExistsException(
                    String.format("username %s already present", user.getUsername()));
        }

        if(userRepository.existsByEmail(user.getEmail())){
            log.warn("email {} already present", user.getEmail());
            throw new EmailAlreadyExistsException(
                    String.format("email %s already present", user.getEmail())
            );
        }

        user.setActive(true);
        user.setPassword(user.getPassword());
        user.setRoles(new HashSet<>(){
            {
                add(Role.USER);
            }
        });

        //persist user object
        User savedUser = userRepository.save(user);
        //send user created event
        userEventSender.sendUserCreatedMessage(savedUser);
        //return the persisted user
        return savedUser;
    }

    public User updateProfilePicture(String newPicUri , String userId){
        log.info("updating profile picture for user {}", userId);

        return userRepository.findById(userId)
                .map(user -> {
                    //get old pic uri for sending update event
                    String oldDisplayPictureUrl = user.getUserProfile().getDisplayPictureUrl();
                    //update user with new pic uri
                    user.getUserProfile().setDisplayPictureUrl(newPicUri);
                    //persist the user
                    User savedUser = userRepository.save(user);

                    //send profile pic updated event
                    userEventSender.sendUserUpdatedMessage(savedUser, oldDisplayPictureUrl);

                    return savedUser;
                }).orElseThrow(
                        () -> new ResourceNotFoundException(
                                String.format("user with id {} not found", userId)));
    }


}
