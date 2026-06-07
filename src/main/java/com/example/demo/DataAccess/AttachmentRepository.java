/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.example.demo.DataAccess;

import com.example.demo.Entities.Attachment;
import com.example.demo.Entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author 2005m
 */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByPageId(Long pageId); 
    
}
