package com.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_record")
public class InspectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pointId;
    private Long inspectorId;
    private String result;
    private String remark;
    private LocalDateTime inspectedAt;
    private Integer syncedFromOffline;
    private LocalDateTime createdAt;
}
