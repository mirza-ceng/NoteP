/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DataAccess;

import com.example.demo.Entities.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author 2005m
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEMail(String eMail);

    boolean existsByEMail(String email);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.password = :password WHERE u.eMail = :eMail")
    void updatePasswordByEmail(@Param("eMail") String eMail, @Param("password") String password);
}
