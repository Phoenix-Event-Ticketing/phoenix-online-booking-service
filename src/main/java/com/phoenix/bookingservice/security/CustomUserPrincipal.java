package com.phoenix.bookingservice.security;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomUserPrincipal {
    private String userId;
    private String email;
    private List<String> permissions;
}