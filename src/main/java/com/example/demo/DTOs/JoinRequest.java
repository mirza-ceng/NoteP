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
public class JoinRequest {

    @NotBlank(message = "Group name can not be empty!!")
    private String groupName;
    @NotBlank(message = "Password can not be empty!!")
    private String password;

    public JoinRequest(String password,String groupName) {
        this.password = password;
        this.groupName=groupName;
    }

    public JoinRequest() {
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
