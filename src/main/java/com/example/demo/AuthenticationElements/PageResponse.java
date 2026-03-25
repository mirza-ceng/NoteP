/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.AuthenticationElements;

/**
 *
 * @author 2005m
 */
public class PageResponse {

    private Long id;
    private String title;
    private String content;
    private Long groupId;
    private Long ownerId;     // Teknik işlemler için 
    private String ownerName;

    public PageResponse(Long id, String title, String content, Long groupId, Long ownerId, String ownerName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.groupId = groupId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

}
