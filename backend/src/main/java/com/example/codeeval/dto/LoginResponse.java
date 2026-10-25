package com.example.codeeval.dto;

/**
 * 登录响应DTO
 */
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserDTO user;

    public LoginResponse() {}

    public LoginResponse(String accessToken, String tokenType, Long expiresIn, UserDTO user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public static LoginResponseBuilder builder() {
        return new LoginResponseBuilder();
    }

    public static class LoginResponseBuilder {
        private String accessToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private UserDTO user;

        public LoginResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public LoginResponseBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public LoginResponseBuilder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public LoginResponseBuilder user(UserDTO user) {
            this.user = user;
            return this;
        }

        public LoginResponse build() {
            return new LoginResponse(accessToken, tokenType, expiresIn, user);
        }
    }
}
