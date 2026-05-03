/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DataAccess;
import com.example.demo.Entities.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 *
 * @author 2005m
 */

public interface GroupRepository extends JpaRepository<Group, Long> {
    
   Optional<Group> findByName(String name);//groupname
   
     List<Group> findByMembersId(Long userID);
    
}
