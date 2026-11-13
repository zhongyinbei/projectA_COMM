package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.dto.ConsumableRequest;
import com.lab.dto.StockRequest;
import com.lab.entity.Consumable;
import com.lab.entity.ConsumableTransaction;
import com.lab.entity.OperationLog;
import com.lab.mapper.ConsumableMapper;
import com.lab.mapper.ConsumableTransactionMapper;
import com.lab.mapper.OperationLogMapper;
import com.lab.service.ConsumableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConsumableServiceImpl implements ConsumableService {

    private final ConsumableMapper consumableMapper;
    private final ConsumableTransactionMapper transactionMapper;
    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<Consumable> page(long page, long size, String keyword) {
        Page<Consumable> pageData = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 1000));
        LambdaQueryWrapper<Consumable> wrapper = new LambdaQueryWrapper<Consumable>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Consumable::getName, keyword)
                        .or().like(Consumable::getSpec, keyword))
                .orderByAsc(Consumable::getId);
        return PageResult.of(consumableMapper.selectPage(pageData, wrapper));
    }

    @Override
    public Consumable create(ConsumableRequest request) {
        if (request == null || !StringUtils.hasText(request.getName()) || !StringUtils.hasText(request.getUnit())) {
            throw new BizException(400, "耗材名称和单位不能为空");
        }
        if (request.getName().trim().length() > 100
                || (request.getSpec() != null && request.getSpec().length() > 100)
                || request.getUnit().trim().length() > 20) {
            throw new BizException(400, "耗材字段长度超出限制");
        }
        int stock = request.getStock() == null ? 0 : request.getStock();
        int threshold = request.getThreshold() == null ? 5 : request.getThreshold();
        if (stock < 0 || threshold < 0) {
            throw new BizException(400, "库存和阈值不能为负数");
        }
        Consumable consumable = new Consumable();
        consumable.setName(request.getName().trim());
        consumable.setSpec(StringUtils.hasText(request.getSpec()) ? request.getSpec().trim() : null);
        consumable.setUnit(request.getUnit().trim());
        consumable.setStock(stock);
        consumable.setThreshold(threshold);
        consumableMapper.insert(consumable);
        return consumableMapper.selectById(consumable.getId());
    }

    @Override
    @Transactional
    public Consumable inbound(Long id, StockRequest request, Long operatorId) {
        int quantity = validQuantity(request);
        if (consumableMapper.selectById(id) == null) {
            throw new BizException(404, "耗材不存在");
        }
        if (consumableMapper.addStock(id, quantity) == 0) {
            throw new BizException(404, "耗材不存在");
        }
        insertTransaction(id, "IN", quantity, operatorId, request.getRemark());
        writeLog("CONSUMABLE", id, "INBOUND", "耗材入库 " + quantity, operatorId);
        return consumableMapper.selectById(id);
    }

    @Override
    @Transactional
    public Consumable outbound(Long id, StockRequest request, Long operatorId) {
        int quantity = validQuantity(request);
        Consumable before = consumableMapper.selectById(id);
        if (before == null) {
            throw new BizException(404, "耗材不存在");
        }
        if (consumableMapper.deductStock(id, quantity) == 0) {
            throw new BizException(400, "库存不足");
        }
        insertTransaction(id, "OUT", quantity, operatorId, request.getRemark());
        Consumable after = consumableMapper.selectById(id);
        writeLog("CONSUMABLE", id, "OUTBOUND", "耗材出库 " + quantity, operatorId);
        if (after.getStock() < after.getThreshold()) {
            writeLog("STOCK_ALERT", id, "LOW_STOCK",
                    after.getName() + " 库存 " + after.getStock() + " 低于阈值 " + after.getThreshold(), operatorId);
        }
        return after;
    }

    private int validQuantity(StockRequest request) {
        if (request == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BizException(400, "数量必须大于 0");
        }
        if (request.getRemark() != null && request.getRemark().length() > 255) {
            throw new BizException(400, "备注不能超过 255 字");
        }
        return request.getQuantity();
    }

    private void insertTransaction(Long consumableId, String type, int quantity, Long operatorId, String remark) {
        ConsumableTransaction transaction = new ConsumableTransaction();
        transaction.setConsumableId(consumableId);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setOperatorId(operatorId);
        transaction.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        transactionMapper.insert(transaction);
    }

    private void writeLog(String bizType, Long bizId, String action, String detail, Long operatorId) {
        OperationLog log = new OperationLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setAction(action);
        log.setDetail(detail);
        log.setOperatorId(operatorId);
        operationLogMapper.insert(log);
    }
}
