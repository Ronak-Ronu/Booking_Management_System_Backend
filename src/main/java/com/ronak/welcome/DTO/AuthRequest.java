package com.ronak.welcome.DTO;

public record AuthRequest(String username, String password,Integer totpCode) {}
