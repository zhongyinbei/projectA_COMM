package com.lab.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InspectionRecordVO {
    private Long id;
    private Long pointId;
    private String pointCode;
    private String location;
    private Long inspectorId;
    private String inspectorName;
    private String result;
    private String remark;
    private LocalDateTime inspectedAt;
    private Integer syncedFromOffline;
    private LocalDateTime createdAt;
}
