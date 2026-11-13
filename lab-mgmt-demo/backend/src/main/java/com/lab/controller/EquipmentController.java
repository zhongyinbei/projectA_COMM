package com.lab.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.EquipmentRequest;
import com.lab.entity.Equipment;
import com.lab.interceptor.RequireRole;
import com.lab.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    @RequireRole({"ADMIN", "REPORTER"})
    public Result<PageResult<Equipment>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status) {
        return Result.ok(equipmentService.page(page, size, keyword, status));
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Equipment> create(@RequestBody EquipmentRequest request) {
        return Result.ok(equipmentService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Equipment> update(@PathVariable Long id, @RequestBody EquipmentRequest request) {
        return Result.ok(equipmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return Result.ok();
    }
}
