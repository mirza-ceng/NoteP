/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DataAccess;

import com.example.demo.Entities.Page;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author 2005m
 */
public interface PageRepository extends JpaRepository<Page, Long> {

    List<Page> findByUserId(Long userId);

    List<Page> findByGroupId(Long groupId);

    List<Page> findByUserIdAndGroupId(Long userId, Long groupId);

}
