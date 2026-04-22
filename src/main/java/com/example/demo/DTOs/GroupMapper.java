/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import com.example.demo.Entities.Group;
import org.springframework.stereotype.Component;

/**
 *
 * @author 2005m
 */
@Component
public class GroupMapper implements IMapper<GroupResponse, Group> {

    @Override
    public GroupResponse toResponse(Group group) {

        return new GroupResponse(group.getId(), group.getName(), group.getMembers(), group.getPages());
    }

    public Group toEntity(GroupRequest gReq) {
        Group group = new Group();
        group.setName(gReq.getName());
        group.setPassword(gReq.getPassword());
        return group;
    }

}
