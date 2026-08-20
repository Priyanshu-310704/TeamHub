package com.example.teamHub.service;

import com.example.teamHub.model.Project;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {
    public List<Project> getProjects(){
        return List.of(new Project(1L,"TeamHum Backend","Build teamhub springboot backend"),new Project(2L,"TeamHub Frontend","build teamhub frontend"));
    }

    public Project getProjectById(Long id){
        if(id==1L){
            return new Project(1L,"TeamHum Backend","Build teamhub springboot backend");
        }
        else if(id==2){
            return new Project(2L,"TeamHub Frontend","build teamhub frontend");
        }
        else{
            return null;
        }
    }
}
