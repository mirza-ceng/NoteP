/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import com.example.demo.Entities.Attachment;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 *
 * @author 2005m
 */
@Component
public class AttachmentMapper implements IMapper<AttachmentResponse, Attachment> {

    @Override
    public AttachmentResponse toResponse(Attachment entity) {
        if (entity == null) {
            return null;
        }
        return new AttachmentResponse(entity.getId(), entity.getFileName(), entity.getFileUrl(), entity.getFileType(), entity.getFileSize(), entity.getCreatedTime());
    }

    public List<AttachmentResponse> toResponseList(List<Attachment> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return (List<AttachmentResponse>) list.stream().map(this::toResponse).collect(Collectors.toList());
    }

}
