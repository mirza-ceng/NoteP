/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DataAccess;

import com.example.demo.Entities.Page;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author 2005m
 */
public interface PageRepository extends JpaRepository<Page, Long> {

    Optional<Page> findByIdAndUserId(Long id, Long userId);

    List<Page> findByUserId(Long userId);

    List<Page> findByGroupId(Long groupId);

    List<Page> findByUserIdAndGroupId(Long userId, Long groupId);

    @Query("SELECT p FROM Page p LEFT JOIN p.group g LEFT JOIN g.members m "
            + "WHERE p.id = :pageId AND (p.user.id = :userId OR m.id = :userId)")
    Optional<Page> findByIdAndUserOrGroupMember(@Param("pageId") Long pageId, @Param("userId") Long userId);
}
