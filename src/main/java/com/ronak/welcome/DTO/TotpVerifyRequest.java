package com.ronak.welcome.DTO;

public class TotpVerifyRequest {
    private String code; // Must be string because user might send '012345'
    // Optional: If you want only self-verification, you get user identity from `Authentication`
    // If you want admin to verify for someone else, add: private String username;

    public TotpVerifyRequest() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
