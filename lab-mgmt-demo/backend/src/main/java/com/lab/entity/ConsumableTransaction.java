package com.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("consumable_transaction")
public class ConsumableTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long consumableId;
    private String type;
    private Integer quantity;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
