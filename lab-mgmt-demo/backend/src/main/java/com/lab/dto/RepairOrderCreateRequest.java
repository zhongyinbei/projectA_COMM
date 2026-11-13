package com.lab.dto;

import lombok.Data;

@Data
public class RepairOrderCreateRequest {
    private Long equipmentId;
    private String description;
    private String imageUrl;
}
