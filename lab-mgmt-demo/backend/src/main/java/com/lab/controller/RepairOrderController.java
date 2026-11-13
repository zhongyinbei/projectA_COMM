package com.lab.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.RepairOrderCompleteRequest;
import com.lab.dto.RepairOrderCreateRequest;
import com.lab.dto.RepairOrderVO;
import com.lab.interceptor.JwtInterceptor;
import com.lab.interceptor.RequireRole;
import com.lab.service.RepairOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repair-orders")
@RequiredArgsConstructor
public class RepairOrderController {

    private final RepairOrderService repairOrderService;

    @PostMapping
    @RequireRole({"REPORTER"})
    public Result<RepairOrderVO> create(@RequestBody RepairOrderCreateRequest request) {
        return Result.ok(repairOrderService.create(JwtInterceptor.currentUserId(), request));
    }

    @GetMapping
    @RequireRole({"ADMIN", "REPAIRER", "REPORTER"})
    public Result<PageResult<RepairOrderVO>> page(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String status) {
        return Result.ok(repairOrderService.page(page, size, status,
                JwtInterceptor.currentUserId(), JwtInterceptor.currentRole()));
    }

    @PatchMapping("/{id}/accept")
    @RequireRole({"REPAIRER"})
    public Result<Void> accept(@PathVariable Long id) {
        repairOrderService.accept(id, JwtInterceptor.currentUserId());
        return Result.ok();
    }

    @PatchMapping("/{id}/reject")
    @RequireRole({"REPAIRER"})
    public Result<Void> reject(@PathVariable Long id) {
        repairOrderService.reject(id, JwtInterceptor.currentUserId());
        return Result.ok();
    }

    @PatchMapping("/{id}/complete")
    @RequireRole({"REPAIRER"})
    public Result<Void> complete(@PathVariable Long id,
                                 @RequestBody(required = false) RepairOrderCompleteRequest request) {
        repairOrderService.complete(id, JwtInterceptor.currentUserId(), request);
        return Result.ok();
    }
}
