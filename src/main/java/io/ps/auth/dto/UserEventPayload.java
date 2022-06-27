package io.ps.auth.dto;


import io.ps.auth.messaging.UserEvents;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserEventPayload {
    private String id;
    private String username;
    private String email;
    private String displayName;
    private String profilePictureUrl;
    private String OldProfilePicUrl;
    private UserEvents userEvents;
}
