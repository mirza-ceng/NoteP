/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 *
 * @author 2005m
 */
public class UserRequest {

    @NotBlank(message = "İsim alanı boş bırakılamaz!")
    @Size(max = 50, message = "İsim en fazla 50 karakter olabilir!")
    private String name;
    @NotBlank(message = "İsim alanı boş bırakılamaz!")
    @Size(max = 50, message = "İsim en fazla 50 karakter olabilir!")
    private String surName;
    @NotBlank(message = "E-posta alanı boş bırakılamaz!")
    @Email(message = "Lütfen geçerli bir e-posta adresi giriniz!")
    @Size(max = 100, message = "E-posta en fazla 100 karakter olabilir!")
    private String eMail;
    @NotBlank(message = "Şifre alanı boş bırakılamaz!")
    @Size(min = 6, max = 100, message = "Şifre en az 6, en fazla 100 karakter olmalıdır!")
    private String password;

    public UserRequest(String name, String surName, String eMail, String password) {
        this.name = name;
        this.surName = surName;
        this.eMail = eMail;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String geteMail() {
        return eMail;
    }

    public void seteMail(String eMail) {
        this.eMail = eMail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
