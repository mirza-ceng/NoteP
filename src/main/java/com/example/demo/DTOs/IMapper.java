/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DTOs;

/**
 *
 * @author 2005m
 */
public interface IMapper <Response,Entity>{
    
    Response toResponse(Entity e);
    Entity updateEntityWithResponse(Entity e,Response r);
    
    
    
    
    
}
