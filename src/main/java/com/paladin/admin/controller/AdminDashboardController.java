package com.paladin.admin.controller;

import com.paladin.admin.Admin;
import com.paladin.common.dto.FeatureRequestDTO;
import com.paladin.common.dto.FeatureRequestStatsDTO;
import com.paladin.common.dto.FeatureRequestStatusUpdateDTO;
import com.paladin.common.enums.FeatureRequestStatus;
import com.paladin.featureRequest.service.FeatureRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final FeatureRequestService featureRequestService;

    // Dashboard home
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal Admin admin, Model model) {
        FeatureRequestStatsDTO stats = featureRequestService.getStats();
        List<FeatureRequestDTO> requests = featureRequestService.getAllFeatureRequests(null, null);

        model.addAttribute("adminUsername", admin.getUsername());
        model.addAttribute("stats", stats);
        model.addAttribute("requests", requests);

        return "admin/dashboard";
    }

    // View single feature request
    @GetMapping("/feature-requests/{id}")
    public String viewFeatureRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal Admin admin,
            Model model) {
        FeatureRequestDTO request = featureRequestService.getFeatureRequestById(id, null);

        model.addAttribute("adminUsername", admin.getUsername());
        model.addAttribute("request", request);
        model.addAttribute("statuses", FeatureRequestStatus.values());

        return "admin/feature-request-detail";
    }

    // Update status (POST from form)
    @PostMapping("/feature-requests/{id}/update-status")
    public String updateStatus(
            @PathVariable UUID id,
            @RequestParam FeatureRequestStatus status,
            @RequestParam(required = false) String adminResponse) {

        FeatureRequestStatusUpdateDTO dto = FeatureRequestStatusUpdateDTO.builder()
                .status(status)
                .adminResponse(adminResponse)
                .build();

        featureRequestService.updateStatus(id, dto);

        return "redirect:/admin/dashboard";
    }
}