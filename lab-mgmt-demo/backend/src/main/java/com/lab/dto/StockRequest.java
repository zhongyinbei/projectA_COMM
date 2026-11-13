package com.lab.dto;

import lombok.Data;

@Data
public class StockRequest {
    private Integer quantity;
    private String remark;
}
