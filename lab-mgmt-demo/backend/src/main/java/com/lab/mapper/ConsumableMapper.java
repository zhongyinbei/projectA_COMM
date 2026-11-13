package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.Consumable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ConsumableMapper extends BaseMapper<Consumable> {

    @Update("UPDATE consumable SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("UPDATE consumable SET stock = stock + #{quantity} WHERE id = #{id}")
    int addStock(@Param("id") Long id, @Param("quantity") int quantity);
}
