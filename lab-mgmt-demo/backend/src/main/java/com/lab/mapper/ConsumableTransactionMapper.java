package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.ConsumableTransaction;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ConsumableTransactionMapper extends BaseMapper<ConsumableTransaction> {

    @Select("SELECT c.name AS name, SUM(t.quantity) AS quantity "
            + "FROM consumable_transaction t JOIN consumable c ON c.id = t.consumable_id "
            + "WHERE t.type = 'OUT' GROUP BY c.id, c.name ORDER BY quantity DESC LIMIT 5")
    List<Map<String, Object>> topOutbound();
}
