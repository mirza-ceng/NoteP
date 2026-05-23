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
public class PageRequest {
    @NotBlank(message = "Not başlığı boş olamaz!")
    @Size(max = 100, message = "Not başlığı en fazla 100 karakter olabilir!")
     private String title;
    @NotBlank(message = "Not içeriği boş olamaz!")
    @Size(max = 10000, message = "Not içeriği çok uzun! En fazla 10.000 karakter yazabilirsiniz.")  
    private String content;

    public PageRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
