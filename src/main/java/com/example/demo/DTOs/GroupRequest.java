/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author 2005m
 */
public class GroupRequest {

    @NotBlank(message = "Group Name can not be empty")
    private String name;
    @NotBlank(message = "Password can not be empty")
    private String password;

    public GroupRequest(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
