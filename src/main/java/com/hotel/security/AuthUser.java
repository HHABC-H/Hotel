package com.hotel.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUser {

    private Long userId;

    private String username;

    private String role;

    private String realName;

    private Integer status;
}