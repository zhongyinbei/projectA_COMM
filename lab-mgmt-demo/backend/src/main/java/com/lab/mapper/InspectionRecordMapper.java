package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.InspectionRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InspectionRecordMapper extends BaseMapper<InspectionRecord> {

    @Select("SELECT DATE_FORMAT(inspected_at, '%Y-%m-%d') AS date, COUNT(*) AS count "
            + "FROM inspection_record WHERE inspected_at >= #{start} "
            + "GROUP BY DATE_FORMAT(inspected_at, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> countDaily(@Param("start") LocalDateTime start);
}
