package com.lab.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.InspectionPointRequest;
import com.lab.dto.InspectionRecordRequest;
import com.lab.dto.InspectionRecordVO;
import com.lab.dto.InspectionSyncRequest;
import com.lab.dto.SyncResult;
import com.lab.entity.InspectionPoint;
import com.lab.interceptor.JwtInterceptor;
import com.lab.interceptor.RequireRole;
import com.lab.service.InspectionPointService;
import com.lab.service.InspectionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionPointService inspectionPointService;
    private final InspectionRecordService inspectionRecordService;

    @GetMapping("/inspection-points")
    @RequireRole({"ADMIN", "REPAIRER"})
    public Result<List<InspectionPoint>> points() {
        return Result.ok(inspectionPointService.list());
    }

    @PostMapping("/inspection-points")
    @RequireRole({"ADMIN"})
    public Result<InspectionPoint> createPoint(@RequestBody InspectionPointRequest request) {
        return Result.ok(inspectionPointService.create(request));
    }

    @GetMapping("/inspection-points/{id}/qrcode")
    @RequireRole({"ADMIN", "REPAIRER"})
    public ResponseEntity<byte[]> qrCode(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(inspectionPointService.qrCode(id));
    }

    @PostMapping("/inspection-records")
    @RequireRole({"REPAIRER"})
    public Result<InspectionRecordVO> createRecord(@RequestBody InspectionRecordRequest request) {
        return Result.ok(inspectionRecordService.createOnline(JwtInterceptor.currentUserId(), request));
    }

    @PostMapping("/inspection-records/sync")
    @RequireRole({"REPAIRER"})
    public Result<SyncResult> sync(@RequestBody InspectionSyncRequest request) {
        return Result.ok(inspectionRecordService.syncOffline(JwtInterceptor.currentUserId(), request));
    }

    @GetMapping("/inspection-records")
    @RequireRole({"ADMIN", "REPAIRER"})
    public Result<PageResult<InspectionRecordVO>> records(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) Long pointId) {
        return Result.ok(inspectionRecordService.page(page, size, pointId));
    }
}
