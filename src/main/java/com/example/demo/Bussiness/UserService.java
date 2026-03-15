/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.AuthenticationElements.LoginRequest;
import com.example.demo.AuthenticationElements.LoginResponse;
import com.example.demo.AuthenticationElements.UserMapper;
import com.example.demo.AuthenticationElements.UserResponse;
import com.example.demo.DataAccess.UserRepository;
import com.example.demo.Entities.User;
import java.util.Optional;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, com.example.demo.AuthenticationElements.UserMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public void register(User user) {
        String password = user.getPassword();
        String encodedPassword = passwordEncoder.encode(password);
        user.setPassword(encodedPassword);
        userRepository.save(user);

    }

    @Transactional
    public LoginResponse logIn(LoginRequest request) {
        User user = userRepository.findByEMail(request.geteMail());
        if (user != null && passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            LoginResponse response = new LoginResponse();
            response.setMessage("BAŞARILI");
            response.setUser(mapper.toResponse(user));
            return response;

        }
        return null;

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
    public void update(UserResponse ur) {
        User originUser = userRepository.findById(ur.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User updatedUser = mapper.updateEntityWithResponse(originUser, ur);

        userRepository.save(updatedUser);

    }

}
