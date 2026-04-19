/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import com.example.demo.DTOs.PageResponse;
import com.example.demo.Entities.Page;
import com.example.demo.Entities.User;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 *
 * @author 2005m
 */
@Component
public class PageMapper implements IMapper<PageResponse, Page> {

    @Override
    public PageResponse toResponse(Page page) {
        User owner = page.getUser();
        Long groupId = (page.getGroup() != null) ? page.getGroup().getId() : null;// could be null!!
        PageResponse response = new PageResponse(page.getId(), page.getTitle(), page.getContent(), groupId, owner.getId(), owner.getName());

        return response;
    }

   
    public Page updateEntityWithResponse(Page existingPage, PageResponse dto) {
        existingPage.setContent(dto.getContent());
        existingPage.setTitle(dto.getTitle());
        existingPage.setLastUpdateDate(LocalDateTime.now());
        return existingPage;

    }

    
    public Page toEntity(PageRequest r) {
        Page page = new Page(r.getTitle(), r.getContent());
        
        return page;

    }

}
