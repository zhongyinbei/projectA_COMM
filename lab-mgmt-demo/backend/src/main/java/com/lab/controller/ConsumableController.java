package com.lab.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.ConsumableRequest;
import com.lab.dto.StockRequest;
import com.lab.entity.Consumable;
import com.lab.interceptor.JwtInterceptor;
import com.lab.interceptor.RequireRole;
import com.lab.service.ConsumableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consumables")
@RequiredArgsConstructor
public class ConsumableController {

    private final ConsumableService consumableService;

    @GetMapping
    @RequireRole({"ADMIN"})
    public Result<PageResult<Consumable>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword) {
        return Result.ok(consumableService.page(page, size, keyword));
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Consumable> create(@RequestBody ConsumableRequest request) {
        return Result.ok(consumableService.create(request));
    }

    @PostMapping("/{id}/inbound")
    @RequireRole({"ADMIN"})
    public Result<Consumable> inbound(@PathVariable Long id, @RequestBody StockRequest request) {
        return Result.ok(consumableService.inbound(id, request, JwtInterceptor.currentUserId()));
    }

    @PostMapping("/{id}/outbound")
    @RequireRole({"ADMIN"})
    public Result<Consumable> outbound(@PathVariable Long id, @RequestBody StockRequest request) {
        return Result.ok(consumableService.outbound(id, request, JwtInterceptor.currentUserId()));
    }
}
