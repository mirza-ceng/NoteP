/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import com.example.demo.DTOs.PageResponse;
import com.example.demo.Entities.Page;
import com.example.demo.Entities.User;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 *
 * @author 2005m
 */
@Component
public class PageMapper implements IMapper<PageResponse, Page> {

    AttachmentMapper attMapper;
    public PageMapper( AttachmentMapper attMapper){
    this.attMapper=attMapper;
    }

    @Override
    public PageResponse toResponse(Page page) {
        User owner = page.getUser();
        Long groupId = (page.getGroup() != null) ? page.getGroup().getId() : null;// could be null!!

        List<AttachmentResponse> attachmentsResponse = attMapper.toResponseList(page.getAttachments());

        PageResponse response = new PageResponse(
                page.getId(), page.getTitle(),
                page.getContent(), groupId,
                owner.getId(), owner.getName(), attachmentsResponse
        );

        return response;
    }

    public List<PageResponse> toResponseList(List<Page> pageList) {
        if (pageList == null) {
            return Collections.emptyList();
        }
        return (List<PageResponse>) pageList.stream().map(this::toResponse).collect(Collectors.toList());

    }

    public Page updateEntityWithResponse(Page existingPage, PageRequest dto) {
        existingPage.setContent(dto.getContent());
        existingPage.setTitle(dto.getTitle());
        existingPage.setLastUpdateDate(LocalDateTime.now());
        return existingPage;

    }
    
    
//findBY olacak şekilde düzenle gerekirse(hangi kafayla yaptınmq)
    public Page toEntity(PageRequest r) {
        Page page = (r == null) ? null : new Page(r.getTitle(), r.getContent());

        return page;

    }

}
