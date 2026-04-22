/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import com.example.demo.Entities.Page;
import com.example.demo.Entities.User;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 2005m
 */
public class GroupResponse {

    private Long id;
    private String name;
    private List<User> members;
     private List<Page> pages;

    public GroupResponse(Long id, String name, List<User> members, List<Page> pages) {
        this.id = id;
        this.name = name;
        this.members = members;
        this.pages = pages;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

}
