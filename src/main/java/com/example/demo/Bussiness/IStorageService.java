/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.example.demo.Bussiness;
import org.springframework.web.multipart.MultipartFile;


/**
 *
 * @author 2005m
 */
public interface IStorageService {

    String uploadFile(MultipartFile file, String folderName);

    void deleteFile(String fileUrl);

}
