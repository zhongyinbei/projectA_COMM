package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.entity.Consumable;
import com.lab.entity.InspectionRecord;
import com.lab.entity.RepairOrder;
import com.lab.mapper.ConsumableMapper;
import com.lab.mapper.ConsumableTransactionMapper;
import com.lab.mapper.EquipmentMapper;
import com.lab.mapper.InspectionRecordMapper;
import com.lab.mapper.RepairOrderMapper;
import com.lab.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EquipmentMapper equipmentMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final ConsumableMapper consumableMapper;
    private final ConsumableTransactionMapper transactionMapper;

    @Override
    public Map<String, Object> summary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("equipmentTotal", equipmentMapper.selectCount(null));
        summary.put("pendingOrders", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, "PENDING")));
        summary.put("abnormalInspections", inspectionRecordMapper.selectCount(new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getResult, "ABNORMAL")));
        summary.put("alertConsumables", alertConsumables());
        return summary;
    }

    @Override
    public List<Map<String, Object>> equipmentStatus() {
        return equipmentMapper.countByStatus();
    }

    @Override
    public List<Map<String, Object>> inspectionRate() {
        // DEMO-SIMPLIFIED: 最近 7 天没有巡检的日期补 0，保证折线图横轴连续。
        LocalDate startDay = LocalDate.now().minusDays(6);
        List<Map<String, Object>> rows = inspectionRecordMapper.countDaily(startDay.atStartOfDay());
        Map<String, Long> counts = new LinkedHashMap<>();
        rows.forEach(row -> counts.put(String.valueOf(row.get("date")), toLong(row.get("count"))));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String date = startDay.plusDays(i).format(DAY);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date);
            item.put("count", counts.getOrDefault(date, 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> consumableTop() {
        return transactionMapper.topOutbound();
    }

    @Override
    public List<Consumable> alertConsumables() {
        return consumableMapper.selectList(new LambdaQueryWrapper<Consumable>()
                .apply("stock < threshold")
                .orderByAsc(Consumable::getId));
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }
}
