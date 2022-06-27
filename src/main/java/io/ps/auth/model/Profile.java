package io.ps.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Profile {

    private String displayName;
    private String displayPictureUrl;
    private Date birthday;
    private Set<Address> addresses;
}
