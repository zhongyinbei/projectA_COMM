package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.EquipmentRequest;
import com.lab.entity.Equipment;

public interface EquipmentService {

    PageResult<Equipment> page(long page, long size, String keyword, String status);

    Equipment create(EquipmentRequest request);

    Equipment update(Long id, EquipmentRequest request);

    void delete(Long id);
}
