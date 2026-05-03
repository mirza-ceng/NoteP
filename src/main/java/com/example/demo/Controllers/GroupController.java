/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Controllers;

/**
 *
 * @author 2005m
 */
import com.example.demo.Bussiness.GroupService;
import com.example.demo.Bussiness.PageService;
import com.example.demo.DTOs.GroupRequest;
import com.example.demo.DTOs.GroupResponse;
import com.example.demo.DTOs.JoinRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> create(@RequestBody GroupRequest groupRequest) {
        groupService.createGroup(groupRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Grup Olusturma Basarılı!");
        return ResponseEntity.ok(response);

    }

    @GetMapping("/my-groups")
    public ResponseEntity<List<GroupResponse>> getMyGroups() {
        return ResponseEntity.ok(groupService.getMyGroups());

    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long id) {

        return ResponseEntity.ok(groupService.getGroupById(id));
    }

    @PostMapping("/join/{id}")
    public ResponseEntity<Map<String, String>> join(@PathVariable Long id, @RequestBody JoinRequest joinReq) {
        groupService.joinGroup(id, joinReq);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Gruba Katılım Basarılı!");
        return ResponseEntity.ok(response);

    }
}
