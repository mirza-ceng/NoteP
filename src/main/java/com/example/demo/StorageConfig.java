/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import java.net.URI;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 *
 * @author 2005m
 */
@Configuration
public class StorageConfig {
    
    @Value("${supabase.s3.endpoint}")
    private String endpoint;
    @Value("${supabase.s3.region}")
    private String region;
    @Value("${supabase.s3.access-key}")
    private String accessKey;
    @Value("${supabase.s3.secret-key}")
    private String secretKey;
    
    @Bean
    S3Client s3Client() {
        
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false) // Ekstra el sıkışma sorunlarını önlemek için
                .build();
        
        return S3Client.builder().endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .crossRegionAccessEnabled(false)
                .serviceConfiguration(serviceConfiguration)
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        
    }
    
}
