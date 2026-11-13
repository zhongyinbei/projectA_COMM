package com.lab.dto;

import lombok.Data;

@Data
public class ConsumableRequest {
    private String name;
    private String spec;
    private String unit;
    private Integer stock;
    private Integer threshold;
}
