/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

import com.example.demo.Entities.Group;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 *
 * @author 2005m
 */
@Component
public class GroupMapper implements IMapper<GroupResponse, Group> {

    private final UserMapper userMapper;
    private final PageMapper pageMapper;

    public GroupMapper(PageMapper pageMapper,UserMapper userMapper) {
        this.pageMapper = pageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public GroupResponse toResponse(Group group) {

        return new GroupResponse(group.getId(),
                group.getName(),
                userMapper.toResponseList(group.getMembers()),
                pageMapper.toResponseList(group.getPages())
        );
    }

    public List<GroupResponse> toResponseList(List<Group> groupList) {
        if (groupList == null) {
            return Collections.emptyList();
        }
        return (List<GroupResponse>) groupList.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Group toEntity(GroupRequest gReq) {
        Group group = new Group();
        group.setName(gReq.getName());
        group.setPassword(gReq.getPassword());
        return group;
    }

}
