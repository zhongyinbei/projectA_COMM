package com.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("consumable")
public class Consumable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String spec;
    private String unit;
    private Integer stock;
    private Integer threshold;
    private LocalDateTime createdAt;
}
