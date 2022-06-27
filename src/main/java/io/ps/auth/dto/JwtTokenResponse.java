package io.ps.auth.dto;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class JwtTokenResponse {

    @NonNull
    private String accessToken;

    private String tokenType = "Bearer";

}
