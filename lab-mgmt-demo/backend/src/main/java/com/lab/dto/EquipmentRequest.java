package com.lab.dto;

import lombok.Data;

@Data
public class EquipmentRequest {
    private String assetNo;
    private String name;
    private String location;
    private String category;
    private String status;
}
