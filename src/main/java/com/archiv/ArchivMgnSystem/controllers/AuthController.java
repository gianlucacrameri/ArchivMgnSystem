package com.archiv.ArchivMgnSystem.controllers;

import com.archiv.ArchivMgnSystem.dtos.LoginRequest;
import com.archiv.ArchivMgnSystem.dtos.RegisterRequest;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Response> registerUser(@RequestBody @Valid RegisterRequest registerRequest, HttpServletRequest request) {
        return ResponseEntity.ok(userService.registerUser(registerRequest, request.getRemoteAddr()));
    }

    @PostMapping("/login")
    public ResponseEntity<Response> loginUser(@RequestBody @Valid LoginRequest loginRequest, HttpServletRequest request) {
        return ResponseEntity.ok(userService.loginUser(loginRequest, request.getRemoteAddr()));
    }
}
