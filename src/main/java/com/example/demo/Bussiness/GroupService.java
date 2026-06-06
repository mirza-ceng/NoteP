/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.DTOs.GroupMapper;
import com.example.demo.DTOs.GroupRequest;
import com.example.demo.DTOs.GroupResponse;
import com.example.demo.DTOs.JoinRequest;
import com.example.demo.DTOs.PageMapper;
import com.example.demo.DTOs.PageRequest;
import com.example.demo.DTOs.PageResponse;
import com.example.demo.DTOs.UserMapper;
import com.example.demo.DataAccess.GroupRepository;
import com.example.demo.DataAccess.PageRepository;
import com.example.demo.DataAccess.UserRepository;
import com.example.demo.Entities.Group;
import com.example.demo.Entities.Page;
import com.example.demo.Entities.User;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author 2005m
 */
@Service
public class GroupService {
//aaa

    private final BCryptPasswordEncoder passwordEncoder;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserMapper userMapper;
    private final PageMapper pageMapper;
    private final GroupMapper groupMapper;

    public GroupService(GroupRepository groupRepository, PageRepository pageRepository, UserRepository userRepository, UserMapper userMapper, PageMapper pageMapper, GroupMapper groupMapper, BCryptPasswordEncoder passwordEncoder) {
        this.groupRepository = groupRepository;
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
        this.pageMapper = pageMapper;
        this.userMapper = userMapper;
        this.groupMapper = groupMapper;
        this.passwordEncoder = passwordEncoder;
    }

    private User getAuthanticatedUser() {
        String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
    }

    @Transactional
    public void createGroup(GroupRequest groupRequest) {
        if (groupRepository.findByName(groupRequest.getName()).isPresent()) {
            throw new RuntimeException("Grup ismi kullanılmış!");

        }

        Group newGroup = groupMapper.toEntity(groupRequest);
        if (groupRequest.getPassword() != null) {
            newGroup.setPassword(passwordEncoder.encode(groupRequest.getPassword()));
        }

        if (newGroup.getMembers() == null) {
            newGroup.setMembers(new ArrayList<User>());
        }

        newGroup.getMembers().add(getAuthanticatedUser());
        groupRepository.save(newGroup);

    }

    @Transactional
    public List<GroupResponse> getMyGroups() {// use for just show groups!

        List<Group> groups = groupRepository.findByMembersId(getAuthanticatedUser().getId());

        return groupMapper.toResponseList(groups);

    }

    @Transactional
    public GroupResponse getGroupById(Long id) {
        Long currentUserId = getAuthanticatedUser().getId();
        Group group = groupRepository.findById(id).orElseThrow(() -> new RuntimeException("Grup Bulunamadı!"));
        boolean isMember = group.getMembers().stream().anyMatch(
                item -> item.getId().equals(currentUserId)
        );
        if (!isMember) {
            throw new RuntimeException("GUVENLIK IHLALI:Uyesı olmadıgınız bır grubun ıcerıgını goremezsınız!");
        }
        GroupResponse groupResponse = new GroupResponse(group.getId(),
                group.getName(),
                userMapper.toResponseList(group.getMembers()),
                pageMapper.toResponseList(group.getPages())
        );
        return groupResponse;
    }

    @Transactional
    public void joinGroup(JoinRequest joinReq) {
        String groupName = joinReq.getGroupName();
        String password = joinReq.getPassword();
        Group group = groupRepository.findByName(groupName).orElseThrow(() -> new RuntimeException("Grup Bulunamadı!"));
        User u = getAuthanticatedUser();
        if (group.getMembers().contains(u)) {
            throw new RuntimeException("KULLANICI ZATEN GRUBA UYE!");
        }
        if (passwordEncoder.matches(password, group.getPassword())) {
            group.getMembers().add(u);
        } else {
            throw new RuntimeException("YANLIS SIFRE GIRDINIZ!");
        }

    }

    @Transactional
    public void leaveFromGroup(Long groupId) {
        User u = getAuthanticatedUser();
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("This group doesn't exist!"));
        boolean isMember = group.getMembers().stream()
                .anyMatch(member -> member.getId().equals(u.getId()));

        if (isMember) {
            group.getMembers().remove(u);
            u.getGroups().remove(group);

        } else {
            throw new RuntimeException("You are not member");
        }
        groupRepository.save(group);

    }

    //PAGE 
    @Transactional
    public List<PageResponse> getPages(Long id) {
        List<Page> pages = pageRepository.findByGroupId(id);
        return pages.stream().map(pageMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void updatePageOfGroup(Long groupId, Long pageId, PageRequest pageReq) {
        User u = getAuthanticatedUser();
        Long userId = u.getId();
        if (groupRepository.isMember(userId, groupId)) {

            Page originPage = pageRepository.findById(pageId).orElseThrow(() -> new ResourceNotFoundException("Page not found "));

            pageMapper.updateEntityWithResponse(originPage, pageReq);

            pageRepository.save(originPage);

        } else {
            throw new RuntimeException("You are not member of this group!");
        }

    }
    
    
    
}
