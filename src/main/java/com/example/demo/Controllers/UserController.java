/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Controllers;

import com.example.demo.AuthenticationElements.LoginRequest;
import com.example.demo.Bussiness.UserService;
import com.example.demo.DTOs.UserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author 2005m
 */
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService UserService;

    public UserController(UserService UserService) {
        this.UserService = UserService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody UserRequest userRequest
    ) {
        UserService.register(userRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Kullanıcı oluşturuldu.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {

        String token = UserService.logIn(loginRequest).getToken();
        return ResponseEntity.ok(token);

    }

    @PutMapping("/update")
    public ResponseEntity<String> update(@RequestBody UserRequest request) {
        UserService.update(request);
        return ResponseEntity.ok("Kullanıcı guncelleme basarılı.");

    }

}
