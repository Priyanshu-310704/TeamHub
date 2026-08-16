package com.example.teamHub.controller;

import com.example.teamHub.model.Project;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    @GetMapping
    public List<Project> getProjects(){
        return List.of(new Project(1L,"TeamHum Backend","Build teamhub springboot backend"),new Project(2L,"TeamHub Frontend","build teamhub frontend"));
    }
}
