package com.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_point")
public class InspectionPoint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String pointCode;
    private String location;
    private LocalDateTime createdAt;
}
