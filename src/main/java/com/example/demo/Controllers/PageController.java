/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Controllers;

import com.example.demo.Bussiness.PageService;
import com.example.demo.DTOs.PageRequest;
import com.example.demo.DTOs.PageResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author 2005m
 */
@RestController
@RequestMapping("/api/pages")
public class PageController {
    
    private final PageService pageService;
    
    public PageController(PageService pageService) {
        this.pageService = pageService;
    }
    
    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> save(@RequestBody PageRequest pageRequest) {
        pageService.savePage(pageRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Not oluşturma başarılı.");
        return ResponseEntity.ok(response);

    }
    
    @GetMapping("/my-list")
    public ResponseEntity<List<PageResponse>> getMyPages() {
        return ResponseEntity.ok(pageService.getMyPages());
        
    }
    
    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, String>> updatePage(@PathVariable Long id, @RequestBody PageResponse dto) {
        pageService.updatePage(id, dto);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Update başarılı.");
        return ResponseEntity.ok(response);

    }
    
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        pageService.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Silme işlemi başarılı.");
        return ResponseEntity.ok(response);
    }
    
}
