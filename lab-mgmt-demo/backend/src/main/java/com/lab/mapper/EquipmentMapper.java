package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.Equipment;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface EquipmentMapper extends BaseMapper<Equipment> {

    @Select("SELECT status, COUNT(*) AS count FROM equipment GROUP BY status ORDER BY status")
    List<Map<String, Object>> countByStatus();
}
