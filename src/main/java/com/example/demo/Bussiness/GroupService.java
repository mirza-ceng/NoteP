/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.DTOs.GroupMapper;
import com.example.demo.DTOs.GroupRequest;
import com.example.demo.DTOs.PageMapper;
import com.example.demo.DTOs.UserMapper;
import com.example.demo.DataAccess.GroupRepository;
import com.example.demo.DataAccess.PageRepository;
import com.example.demo.DataAccess.UserRepository;
import com.example.demo.Entities.Group;
import com.example.demo.Entities.User;
import java.util.ArrayList;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author 2005m
 */
@Service
public class GroupService {

    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserMapper userMapper;
    private final PageMapper pageMapper;
    private final GroupMapper groupMapper;

    public GroupService(GroupRepository groupRepository,
            PageRepository pageRepository,
            UserRepository userRepository,
            UserMapper userMapper, PageMapper pageMapper,
            GroupMapper groupMapper
    ) {
        this.groupRepository = groupRepository;
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
        this.pageMapper = pageMapper;
        this.userMapper = userMapper;
        this.groupMapper = groupMapper;
    }

    private User getAuthanticatedUser() {
        String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
    }

    private String getAuthanticatedUserEMail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public void createGroup(GroupRequest groupRequest) {
        if (groupRepository.findByName(groupRequest.getName()).isPresent()) {
            throw new RuntimeException("Grup ismi kullanılmış!");
            
        }
        ---------------
        Group newGroup=groupMapper.toEntity(groupRequest);
        
        
        if (newGroup.getMembers()==null) {
            newGroup.setMembers(new ArrayList<User>());
        }
        
          newGroup.getMembers().add(getAuthanticatedUser());
        groupRepository.save(newGroup);
        
        
        
    }

    //getMyGroups,getSpecificGroup,save,delete,update,getPagesOfGroup
}
