package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.ConsumableRequest;
import com.lab.dto.StockRequest;
import com.lab.entity.Consumable;

public interface ConsumableService {

    PageResult<Consumable> page(long page, long size, String keyword);

    Consumable create(ConsumableRequest request);

    Consumable inbound(Long id, StockRequest request, Long operatorId);

    Consumable outbound(Long id, StockRequest request, Long operatorId);
}
