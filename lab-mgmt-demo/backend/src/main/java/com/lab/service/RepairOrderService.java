package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.RepairOrderCompleteRequest;
import com.lab.dto.RepairOrderCreateRequest;
import com.lab.dto.RepairOrderVO;

public interface RepairOrderService {

    RepairOrderVO create(Long reporterId, RepairOrderCreateRequest request);

    PageResult<RepairOrderVO> page(long page, long size, String status, Long userId, String role);

    void accept(Long id, Long repairerId);

    void reject(Long id, Long repairerId);

    void complete(Long id, Long repairerId, RepairOrderCompleteRequest request);
}
