package com.lab.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairOrderVO {
    private Long id;
    private String orderNo;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentAssetNo;
    private Long reporterId;
    private String reporterName;
    private Long assigneeId;
    private String assigneeName;
    private String description;
    private String imageUrl;
    private String status;
    private String repairNote;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
