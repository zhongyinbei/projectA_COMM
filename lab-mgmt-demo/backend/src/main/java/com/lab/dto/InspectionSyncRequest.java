package com.lab.dto;

import lombok.Data;

import java.util.List;

@Data
public class InspectionSyncRequest {
    private List<InspectionRecordRequest> records;
}
