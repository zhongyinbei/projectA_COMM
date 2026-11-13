package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.dto.EquipmentRequest;
import com.lab.entity.Equipment;
import com.lab.entity.RepairOrder;
import com.lab.mapper.EquipmentMapper;
import com.lab.mapper.RepairOrderMapper;
import com.lab.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private static final Set<String> STATUSES = Set.of("NORMAL", "REPAIRING", "SCRAPPED");

    private final EquipmentMapper equipmentMapper;
    private final RepairOrderMapper repairOrderMapper;

    @Override
    public PageResult<Equipment> page(long page, long size, String keyword, String status) {
        Page<Equipment> pageData = new Page<>(normalizePage(page), normalizeSize(size));
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<Equipment>()
                .eq(StringUtils.hasText(status), Equipment::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Equipment::getName, keyword)
                        .or().like(Equipment::getAssetNo, keyword)
                        .or().like(Equipment::getLocation, keyword))
                .orderByDesc(Equipment::getId);
        return PageResult.of(equipmentMapper.selectPage(pageData, wrapper));
    }

    @Override
    public Equipment create(EquipmentRequest request) {
        validate(request);
        ensureAssetNoUnique(request.getAssetNo().trim(), null);
        Equipment equipment = new Equipment();
        apply(equipment, request);
        equipmentMapper.insert(equipment);
        return equipmentMapper.selectById(equipment.getId());
    }

    @Override
    public Equipment update(Long id, EquipmentRequest request) {
        Equipment existing = equipmentMapper.selectById(id);
        if (existing == null) {
            throw new BizException(404, "设备不存在");
        }
        validate(request);
        ensureAssetNoUnique(request.getAssetNo().trim(), id);
        apply(existing, request);
        equipmentMapper.updateById(existing);
        return equipmentMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        if (equipmentMapper.selectById(id) == null) {
            throw new BizException(404, "设备不存在");
        }
        long references = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getEquipmentId, id));
        if (references > 0) {
            throw new BizException(409, "该设备存在报修工单，不能删除");
        }
        equipmentMapper.deleteById(id);
    }

    private void validate(EquipmentRequest request) {
        if (request == null) {
            throw new BizException(400, "请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getAssetNo()) || !StringUtils.hasText(request.getName())
                || !StringUtils.hasText(request.getLocation())) {
            throw new BizException(400, "资产编号、设备名称和位置不能为空");
        }
        if (request.getAssetNo().trim().length() > 50 || request.getName().trim().length() > 100
                || request.getLocation().trim().length() > 100
                || (request.getCategory() != null && request.getCategory().length() > 50)) {
            throw new BizException(400, "设备字段长度超出限制");
        }
        if (StringUtils.hasText(request.getStatus()) && !STATUSES.contains(request.getStatus())) {
            throw new BizException(400, "设备状态不合法");
        }
    }

    private void ensureAssetNoUnique(String assetNo, Long excludeId) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getAssetNo, assetNo)
                .ne(excludeId != null, Equipment::getId, excludeId);
        if (equipmentMapper.selectCount(wrapper) > 0) {
            throw new BizException(409, "资产编号已存在");
        }
    }

    private void apply(Equipment equipment, EquipmentRequest request) {
        equipment.setAssetNo(request.getAssetNo().trim());
        equipment.setName(request.getName().trim());
        equipment.setLocation(request.getLocation().trim());
        equipment.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : null);
        equipment.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "NORMAL");
    }

    private long normalizePage(long page) {
        return Math.max(page, 1);
    }

    private long normalizeSize(long size) {
        return Math.min(Math.max(size, 1), 1000);
    }
}
