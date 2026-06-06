/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.example.demo.DTOs;

import com.example.demo.DTOs.UserResponse;
import com.example.demo.DTOs.UserRequest;
import com.example.demo.Entities.User;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.hibernate.mapping.Collection;
import org.springframework.stereotype.Component;

/**
 *
 * @author 2005m
 */
@Component // Spring'in bu sınıfı yönetmesi için şart
public class UserMapper implements IMapper<UserResponse, User> {

    @Override
    public UserResponse toResponse(User u) {
        UserResponse ur = new UserResponse(u.getId(), u.getName(), u.getSurName(), u.geteMail());
        return ur;

    }

    public UserRequest toRequest(User u) {

        return new UserRequest(u.getName(), u.getSurName(), u.geteMail(), u.getPassword());

    }

    public List<UserResponse> toResponseList(List<User> userList) {
        if (userList == null) {
            return Collections.emptyList();
        }
        return (List<UserResponse>) userList.stream().map(this::toResponse).collect(Collectors.toList());
    }



    public User toEntity(UserRequest r) {
        User u = new User(r.getName(), r.getSurName(), r.geteMail(), r.getPassword());
        return u;
    }

}
