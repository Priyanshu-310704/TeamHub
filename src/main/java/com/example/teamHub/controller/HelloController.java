package com.example.teamHub.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {
    @GetMapping("/health")
    public ResponseEntity<String> getHealth(){
        return ResponseEntity.status(HttpStatus.OK).body("TeamHub is Running");
    }
}
