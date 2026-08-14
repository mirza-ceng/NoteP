/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.IOException;
import java.util.UUID;

/**
 *
 * @author 2005m
 */
@Service
public class SupabaseServiceImpl implements IStorageService {

    private final S3Client s3Client;
    @Value("${supabase.s3.bucket-name}")
    private String bucketName;
    @Value("${supabase.s3.endpoint}")
    private String s3Endpoint;

    public SupabaseServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadFile(MultipartFile file, String folderName) {
        String originalFileName = file.getOriginalFilename();
        String extension = (originalFileName != null && originalFileName.contains(".")) ? originalFileName.substring(originalFileName.lastIndexOf(".")) : "";
        String key = folderName + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String projectUrl = s3Endpoint.substring(0, s3Endpoint.indexOf("/storage/v1/s3"));
            return projectUrl + "/storage/v1/object/public/" + bucketName + "/" + key;

        } catch (Exception ex) {
            throw new RuntimeException("Dosya Yükleme Hatası :" + ex.getMessage());
        }

    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String marker = "/public/" + bucketName + "/";
            if (!fileUrl.contains(marker)) {
                return;
            }
            String key = fileUrl.substring(fileUrl.indexOf(marker) + marker.length());
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            System.err.println("Dosya Silme Hatası :" + e.getMessage());
        }
    }

}
