/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Controllers;

import com.example.demo.Bussiness.PageService;
import com.example.demo.DTOs.AttachmentResponse;
import com.example.demo.DTOs.PageRequest;
import com.example.demo.DTOs.PageResponse;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author 2005m
 */
@RestController
@RequestMapping("/api/pages")
@CrossOrigin(origins = "*")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> save(@Valid @RequestBody PageRequest pageRequest) {
        pageService.savePage(pageRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Not oluşturma başarılı.");
        return ResponseEntity.ok(response);

    }

    @GetMapping("/my-list")
    public ResponseEntity<List<PageResponse>> getMyPages() {
        return ResponseEntity.ok(pageService.getMyPages());

    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updatePage(@PathVariable Long id, @Valid @RequestBody PageRequest dto) {
        pageService.updatePage(id, dto);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Update başarılı.");
        return ResponseEntity.ok(response);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        pageService.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Silme işlemi başarılı.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{pageId}/add-to-group/{groupId}")
    public ResponseEntity<Map<String, String>> addToGroup(@PathVariable Long pageId, @PathVariable Long groupId) {
        pageService.addToGroup(pageId, groupId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Not başarıyla gruba dahil edildi.");
        return ResponseEntity.ok(response);

    }

    @PutMapping("/{pageId}/remove-from-group")
    public ResponseEntity<Map<String, String>> removeFromGroup(@PathVariable Long pageId) {
        pageService.removeFromGroup(pageId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Not gruptan cıkarıldı.");
        return ResponseEntity.ok(response);
    }
    //File 
    @PostMapping(value = "/{pageId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadFile(@PathVariable Long pageId, @RequestParam("file") MultipartFile file) {
        AttachmentResponse response = pageService.uploadAttachmentToResponse(pageId, file);
        return ResponseEntity.ok(response);

    }

    @DeleteMapping("/{pageId}/attachments/{attachmentId}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable Long pageId, @PathVariable Long attachmentId) {
        pageService.deleteAttachmentFromPage(pageId, attachmentId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Dosya Silme Basarili");
        return ResponseEntity.ok(response);
    }
}
