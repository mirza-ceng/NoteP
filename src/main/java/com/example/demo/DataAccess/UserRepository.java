/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DataAccess;

import com.example.demo.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author 2005m
 */
public interface UserRepository extends JpaRepository<User, Long> {

 User findByEMail(String eMail);
}
