package io.ps.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
