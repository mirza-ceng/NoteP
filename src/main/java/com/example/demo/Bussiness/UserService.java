/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.AuthenticationElements.JWTUtil;
import com.example.demo.AuthenticationElements.LoginRequest;
import com.example.demo.AuthenticationElements.LoginResponse;
import com.example.demo.DTOs.UserMapper;
import com.example.demo.DTOs.UserRequest;
import com.example.demo.DTOs.UserResponse;
import com.example.demo.DataAccess.UserRepository;
import com.example.demo.Entities.User;
import java.util.Optional;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author 2005m
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    public UserService(UserRepository userRepository, UserMapper mapper, JWTUtil jwtUtil, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    private User getAuthanticatedUser() {
        String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
    }

    @Transactional
    public void register(UserRequest userRequest) {
        try {
            User user = mapper.toEntity(userRequest);
            System.out.println("User created: " + user.geteMail());

            if (userRepository.existsByEMail(user.geteMail())) {
                throw new RuntimeException("This User Already Existed.");
            }
            String password = user.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            user.setPassword(encodedPassword);
            User savedUser = userRepository.save(user);
            System.out.println("User saved: " + savedUser.geteMail() + " ID: " + savedUser.getId());
        } catch (Exception e) {
            System.out.println("Exception in register: " + e.getMessage());
            throw e;
        }

    }

    @Transactional
    public LoginResponse logIn(LoginRequest request) {
        User user = userRepository.findByEMail(request.geteMail()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            LoginResponse response = new LoginResponse();
            String token = jwtUtil.generateToken(user.geteMail());
            response.setMessage("BAŞARILI");
            response.setUser(mapper.toResponse(user));
            response.setToken(token);
            return response;

        }
        throw new RuntimeException("Hatalı şifre ya da e-posta!");

    }

    @Transactional
    public UserResponse getById(long id) {
        return userRepository.findById(id).map(mapper::toResponse).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public void deleteById(long id) {
        userRepository.deleteById(id);
    }

    @Transactional//tamam
    public void update(UserRequest ur) {
      
            User originUser = getAuthanticatedUser();
            User updatedUser = mapper.updateEntityWithRequest(originUser, ur);

            userRepository.save(updatedUser);
        

    }

}
