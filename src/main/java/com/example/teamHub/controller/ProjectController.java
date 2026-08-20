package com.example.teamHub.controller;

import com.example.teamHub.model.Project;
import com.example.teamHub.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    ProjectController(ProjectService projectService){
        this.projectService=projectService;
    }

    @GetMapping
    public List<Project> getProjects(){
        return projectService.getProjects();
    }

    @GetMapping("/{id}")
    public Project getProject(@PathVariable Long id){
        return projectService.getProjectById(id);
    }
}
