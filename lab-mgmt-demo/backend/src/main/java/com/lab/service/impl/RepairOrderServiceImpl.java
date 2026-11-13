package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.dto.RepairOrderCompleteRequest;
import com.lab.dto.RepairOrderCreateRequest;
import com.lab.dto.RepairOrderVO;
import com.lab.entity.Equipment;
import com.lab.entity.OperationLog;
import com.lab.entity.RepairOrder;
import com.lab.entity.SysUser;
import com.lab.mapper.EquipmentMapper;
import com.lab.mapper.OperationLogMapper;
import com.lab.mapper.RepairOrderMapper;
import com.lab.mapper.SysUserMapper;
import com.lab.service.RepairOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairOrderServiceImpl implements RepairOrderService {

    private static final Set<String> STATUSES = Set.of("PENDING", "ACCEPTED", "REJECTED", "COMPLETED");
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RepairOrderMapper repairOrderMapper;
    private final EquipmentMapper equipmentMapper;
    private final SysUserMapper userMapper;
    private final OperationLogMapper operationLogMapper;

    @Override
    @Transactional
    public RepairOrderVO create(Long reporterId, RepairOrderCreateRequest request) {
        if (request == null || request.getEquipmentId() == null) {
            throw new BizException(400, "请选择报修设备");
        }
        if (!StringUtils.hasText(request.getDescription())) {
            throw new BizException(400, "故障描述不能为空");
        }
        if (request.getDescription().length() > 500) {
            throw new BizException(400, "故障描述不能超过 500 字");
        }
        if (request.getImageUrl() != null && request.getImageUrl().length() > 255) {
            throw new BizException(400, "图片地址过长");
        }
        Equipment equipment = equipmentMapper.selectById(request.getEquipmentId());
        if (equipment == null) {
            throw new BizException(404, "设备不存在");
        }
        if ("SCRAPPED".equals(equipment.getStatus())) {
            throw new BizException(400, "已报废设备不能报修");
        }

        RepairOrder order = new RepairOrder();
        order.setOrderNo(generateOrderNo());
        order.setEquipmentId(request.getEquipmentId());
        order.setReporterId(reporterId);
        order.setDescription(request.getDescription().trim());
        order.setImageUrl(StringUtils.hasText(request.getImageUrl()) ? request.getImageUrl().trim() : null);
        order.setStatus("PENDING");
        repairOrderMapper.insert(order);

        RepairOrder saved = repairOrderMapper.selectById(order.getId());
        return toVo(saved, Map.of(equipment.getId(), equipment),
                Map.of(reporterId, userMapper.selectById(reporterId)));
    }

    @Override
    public PageResult<RepairOrderVO> page(long page, long size, String status, Long userId, String role) {
        if (StringUtils.hasText(status) && !STATUSES.contains(status)) {
            throw new BizException(400, "工单状态不合法");
        }
        Page<RepairOrder> pageData = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 1000));
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        if ("REPORTER".equals(role)) {
            wrapper.eq(RepairOrder::getReporterId, userId);
        } else if ("REPAIRER".equals(role)) {
            wrapper.and(w -> w.eq(RepairOrder::getStatus, "PENDING")
                    .or().eq(RepairOrder::getAssigneeId, userId));
        } else if (!"ADMIN".equals(role)) {
            throw new BizException(403, "无权限访问");
        }
        wrapper.eq(StringUtils.hasText(status), RepairOrder::getStatus, status)
                .orderByDesc(RepairOrder::getCreatedAt)
                .orderByDesc(RepairOrder::getId);

        Page<RepairOrder> result = repairOrderMapper.selectPage(pageData, wrapper);
        Map<Long, Equipment> equipmentMap = loadEquipments(result.getRecords());
        Map<Long, SysUser> userMap = loadUsers(result.getRecords());
        List<RepairOrderVO> records = result.getRecords().stream()
                .map(order -> toVo(order, equipmentMap, userMap))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void accept(Long id, Long repairerId) {
        LocalDateTime now = LocalDateTime.now();
        if (repairOrderMapper.accept(id, repairerId, now) == 0) {
            throw new BizException(409, "工单状态已变化");
        }
        RepairOrder order = repairOrderMapper.selectById(id);
        equipmentMapper.update(null, new LambdaUpdateWrapper<Equipment>()
                .eq(Equipment::getId, order.getEquipmentId())
                .set(Equipment::getStatus, "REPAIRING"));
        writeLog(order.getId(), "ACCEPTED", "维修员接单", repairerId);
    }

    @Override
    @Transactional
    public void reject(Long id, Long repairerId) {
        if (repairOrderMapper.reject(id) == 0) {
            throw new BizException(409, "工单状态已变化");
        }
        writeLog(id, "REJECTED", "维修员拒绝工单", repairerId);
    }

    @Override
    @Transactional
    public void complete(Long id, Long repairerId, RepairOrderCompleteRequest request) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(404, "工单不存在");
        }
        if (order.getAssigneeId() == null || !order.getAssigneeId().equals(repairerId)) {
            throw new BizException(403, "只能完成自己接单的工单");
        }
        String repairNote = request == null ? null : request.getRepairNote();
        if (repairNote != null && repairNote.length() > 500) {
            throw new BizException(400, "维修说明不能超过 500 字");
        }
        if (repairOrderMapper.complete(id, repairNote, LocalDateTime.now()) == 0) {
            throw new BizException(409, "工单状态已变化");
        }
        equipmentMapper.update(null, new LambdaUpdateWrapper<Equipment>()
                .eq(Equipment::getId, order.getEquipmentId())
                .set(Equipment::getStatus, "NORMAL"));
        writeLog(id, "COMPLETED", repairNote, repairerId);
    }

    private String generateOrderNo() {
        return "RO" + LocalDateTime.now().format(ORDER_NO_TIME)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private void writeLog(Long orderId, String action, String detail, Long operatorId) {
        OperationLog log = new OperationLog();
        log.setBizType("REPAIR_ORDER");
        log.setBizId(orderId);
        log.setAction(action);
        log.setDetail(detail);
        log.setOperatorId(operatorId);
        operationLogMapper.insert(log);
    }

    private Map<Long, Equipment> loadEquipments(List<RepairOrder> orders) {
        Set<Long> ids = orders.stream().map(RepairOrder::getEquipmentId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return equipmentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Equipment::getId, Function.identity()));
    }

    private Map<Long, SysUser> loadUsers(List<RepairOrder> orders) {
        Set<Long> ids = orders.stream()
                .flatMap(order -> java.util.stream.Stream.of(order.getReporterId(), order.getAssigneeId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private RepairOrderVO toVo(RepairOrder order, Map<Long, Equipment> equipmentMap, Map<Long, SysUser> userMap) {
        RepairOrderVO vo = new RepairOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setEquipmentId(order.getEquipmentId());
        Equipment equipment = equipmentMap.get(order.getEquipmentId());
        if (equipment != null) {
            vo.setEquipmentName(equipment.getName());
            vo.setEquipmentAssetNo(equipment.getAssetNo());
        }
        vo.setReporterId(order.getReporterId());
        SysUser reporter = userMap.get(order.getReporterId());
        if (reporter != null) {
            vo.setReporterName(reporter.getRealName());
        }
        vo.setAssigneeId(order.getAssigneeId());
        SysUser assignee = order.getAssigneeId() == null ? null : userMap.get(order.getAssigneeId());
        if (assignee != null) {
            vo.setAssigneeName(assignee.getRealName());
        }
        vo.setDescription(order.getDescription());
        vo.setImageUrl(order.getImageUrl());
        vo.setStatus(order.getStatus());
        vo.setRepairNote(order.getRepairNote());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setAcceptedAt(order.getAcceptedAt());
        vo.setCompletedAt(order.getCompletedAt());
        return vo;
    }
}
