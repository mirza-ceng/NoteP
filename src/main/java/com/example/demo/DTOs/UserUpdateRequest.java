/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 *
 * @author 2005m
 */
public class UserUpdateRequest {

    @NotBlank(message = "Şifre alanı boş bırakılamaz!")
    @Size(min = 6, max = 100, message = "Şifre en az 6, en fazla 100 karakter olmalıdır!")
    private String password;

    public UserUpdateRequest(String password) {
        this.password = password;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

}
