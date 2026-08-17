/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import com.example.demo.Entities.Role;
import java.time.LocalDateTime;

/**
 *
 * @author 2005m
 */
public record ChatMessageResponse(
        Long id,
        Role role,
        String content,
        LocalDateTime createdDate
        ) {

}
