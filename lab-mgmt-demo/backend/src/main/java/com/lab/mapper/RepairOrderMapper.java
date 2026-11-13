package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.RepairOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    @Update("UPDATE repair_order SET status = 'ACCEPTED', assignee_id = #{assigneeId}, accepted_at = #{acceptedAt} "
            + "WHERE id = #{id} AND status = 'PENDING'")
    int accept(@Param("id") Long id,
               @Param("assigneeId") Long assigneeId,
               @Param("acceptedAt") LocalDateTime acceptedAt);

    @Update("UPDATE repair_order SET status = 'REJECTED' WHERE id = #{id} AND status = 'PENDING'")
    int reject(@Param("id") Long id);

    @Update("UPDATE repair_order SET status = 'COMPLETED', repair_note = #{repairNote}, completed_at = #{completedAt} "
            + "WHERE id = #{id} AND status = 'ACCEPTED'")
    int complete(@Param("id") Long id,
                 @Param("repairNote") String repairNote,
                 @Param("completedAt") LocalDateTime completedAt);
}
