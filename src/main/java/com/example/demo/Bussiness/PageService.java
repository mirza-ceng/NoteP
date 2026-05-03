/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author 2005m
 */
@Service
public class PageService {

    private final GroupRepository groupRepository;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PageMapper pageMapper;

    public PageService(PageRepository pageRepository, UserRepository userRepository, UserMapper userMapper, PageMapper pageMapper, GroupRepository groupRepository) {
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.pageMapper = pageMapper;
        this.groupRepository = groupRepository;

    }

    private User getAuthanticatedUser() {
        String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
    }

    private String getAuthanticatedUserEMail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public void savePage(PageRequest dto) {

        User u = getAuthanticatedUser();
        //Page page = pageMapper.toEntity(dto);
        Page page = new Page(dto.getTitle(), dto.getContent());
        page.setUser(null);
        page.setUser(u);
        pageRepository.save(page);

    }

    @Transactional
    public List<PageResponse> getMyPages() {

        User u = getAuthanticatedUser();

        List<Page> pages = pageRepository.findByUserId(u.getId());

        return pages.stream().map(pageMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(Long id) {
        String eMail = getAuthanticatedUserEMail();
        Page page = pageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Page not found with id: " + eMail));
        if (page.getUser().geteMail().equals(eMail)) {
            pageRepository.deleteById(id);
        } else {
            throw new RuntimeException("GÜVENLİK İHLALİ: Başkasına ait bir notu silemezsiniz!");
        }

    }

    @Transactional
    public void updatePage(Long id, PageResponse dto) {

        User u = getAuthanticatedUser();

        Page originPage = pageRepository.findByIdAndUserId(id, u.getId()).orElseThrow(() -> new ResourceNotFoundException("Page not found "));

        pageMapper.updateEntityWithResponse(originPage, dto);

        pageRepository.save(originPage);

    }

    /**
     * Mevcut bir notu bir gruba dahil eder.
     *
     * @param pageId Gruba eklenecek notun ID'si
     * @param groupId Hedef grubun ID'si
     */
    @Transactional
    public void addToGroup(Long pageId, Long groupId) {
        User u = getAuthanticatedUser();
        Page page = pageRepository.findByIdAndUserId(pageId, u.getId()).orElseThrow(() -> new RuntimeException("Page is not exist or You are not owner. "));
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group is not found."));

        if (group.getMembers().contains(u)) {
            page.setGroup(group);
            if (!group.getPages().contains(page)) {
                group.getPages().add(page);
            }
            pageRepository.save(page);
            groupRepository.save(group);

        } else {
            throw new RuntimeException("You are not member of this group!");
        }

    }

    @Transactional
    public void removeFromGroup(Long pageId) {
        User u = getAuthanticatedUser();
        Page page = pageRepository.findByIdAndUserId(pageId, u.getId()).orElseThrow(() -> new RuntimeException("Page is not exist or You are not owner. "));
        Group group = page.getGroup();
        if (group == null) {
            throw new RuntimeException("Page doesn't have a group");
        }

        if (group.getMembers().contains(u)) {
            page.setGroup(null);
            if (group.getPages().contains(page)) {
                group.getPages().remove(page);
            }
            pageRepository.save(page);
            groupRepository.save(group);

        } else {
            throw new RuntimeException("You are not member of this group!");
        }

    }

}
