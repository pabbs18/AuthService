package io.ps.auth.messaging;

import io.ps.auth.dto.UserEventPayload;
import io.ps.auth.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserEventSender {

    @Autowired
    UserEventChannel userEventChannel;

    public void sendUserCreatedMessage(User user){
        sendUserChangedEvent(buildUserEventPayload(user, UserEvents.CREATED));
        log.info("User-Created-Event sent for the user {}",
                user.getUsername());
    }

    public void sendUserUpdatedMessage(User user){
        sendUserChangedEvent(buildUserEventPayload(user, UserEvents.UPDATED));
        log.info("User-Updated-Event sent for the user {}",
                user.getUsername());
    }

    public void sendUserUpdatedMessage(User user, String oldProfilePicUrl){
        UserEventPayload payload = buildUserEventPayload(user, UserEvents.UPDATED);
        payload.setOldProfilePicUrl(oldProfilePicUrl);
        sendUserChangedEvent(payload);
        log.info("User-Updated-Event sent for the user {}",
                user.getUsername());
    }

    private void sendUserChangedEvent(UserEventPayload payload){
        // build the message to be sent
        Message<UserEventPayload> message = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getId())
                .build();

        //send the message on userevent channel
        userEventChannel.userChanged().send(message);

        //log the event
        log.info("even {} sent to tpoic {} for user {}",
                //message.getPayload().getUserEvents().name(),
                userEventChannel.OUTPUT,
                message.getPayload().getUsername());
    }

    private UserEventPayload buildUserEventPayload(User user, UserEvents userEvents){
        return UserEventPayload
                .builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getUserProfile().getDisplayName())
                .profilePictureUrl(user.getUserProfile().getDisplayPictureUrl())
                .build();
    }
}
