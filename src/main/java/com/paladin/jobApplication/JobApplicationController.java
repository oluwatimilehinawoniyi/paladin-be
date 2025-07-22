package com.paladin.jobApplication;

import com.paladin.dto.JobApplicationDTO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController("/api/v1/applications")
public class JobApplicationController {

    @GetMapping
    public List<JobApplicationDTO> getApplications() {
        return new ArrayList<>();
    }

    @PostMapping("/send")
    public void sendApplication(
            @RequestBody JobApplicationDTO dto) {}

    @PutMapping("/{id}")
    public JobApplicationDTO updateApplication(
            @RequestBody JobApplicationDTO dto,
            @PathVariable UUID id) {
        return dto;
    }
}
