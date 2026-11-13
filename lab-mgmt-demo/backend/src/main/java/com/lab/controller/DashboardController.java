package com.lab.controller;

import com.lab.common.Result;
import com.lab.interceptor.RequireRole;
import com.lab.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(dashboardService.summary());
    }

    @GetMapping("/equipment-status")
    public Result<List<Map<String, Object>>> equipmentStatus() {
        return Result.ok(dashboardService.equipmentStatus());
    }

    @GetMapping("/inspection-rate")
    public Result<List<Map<String, Object>>> inspectionRate() {
        return Result.ok(dashboardService.inspectionRate());
    }

    @GetMapping("/consumable-top")
    public Result<List<Map<String, Object>>> consumableTop() {
        return Result.ok(dashboardService.consumableTop());
    }
}
