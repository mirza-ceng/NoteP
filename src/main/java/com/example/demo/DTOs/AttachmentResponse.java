/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import java.time.LocalDateTime;

/**
 *
 * @author 2005m
 */
public record AttachmentResponse(
        Long id,
        String fileName,
        String fileUrl,
        String fileType,
        Long fileSize,
        LocalDateTime createdTime) {

}
