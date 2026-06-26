/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 *
 * @author 2005m
 */
public record ChatRequest(
        @NotBlank(message = "Mesaj alanı bos bırakılamaz!!")
        String message,
        List<Long> pageIds
        ) {

}
