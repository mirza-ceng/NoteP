/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.AuthenticationElements.UserMapper;
import com.example.demo.DataAccess.PageRepository;
import com.example.demo.DataAccess.UserRepository;
import com.example.demo.Entities.Page;
import com.example.demo.Entities.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author 2005m
 */
public class PageService {

    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public PageService(PageRepository pageRepository, UserRepository userRepository, UserMapper userMapper) {
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;

    }

    private String getAuthanticatedUserEMail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public void savePage(Page page) {

        String eMail = getAuthanticatedUserEMail();
        User u = userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
        page.setUser(u);
        pageRepository.save(page);

    }

    @Transactional
    public List<Page> getMyPages() {
        String eMail = getAuthanticatedUserEMail();
        User u = userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));

        return pageRepository.findByUserId(u.getId());
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
    public void updatePage(Long id, Page updatedPage) {
        String eMail = getAuthanticatedUserEMail();
        User u = userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));

        Page originPage = pageRepository.findByIdAndUserId(id, u.getId()).orElseThrow(() -> new ResourceNotFoundException("Page not found "));

        originPage.setContent(updatedPage.getContent());
        originPage.setLastUpdateDate(LocalDateTime.now());
        originPage.setTitle(updatedPage.getTitle());
        pageRepository.save(originPage);

    }

}
