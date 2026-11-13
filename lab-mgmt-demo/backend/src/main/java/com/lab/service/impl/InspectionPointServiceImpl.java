package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.BizException;
import com.lab.dto.InspectionPointRequest;
import com.lab.entity.InspectionPoint;
import com.lab.mapper.InspectionPointMapper;
import com.lab.service.InspectionPointService;
import com.lab.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionPointServiceImpl implements InspectionPointService {

    private final InspectionPointMapper inspectionPointMapper;

    @Override
    public List<InspectionPoint> list() {
        return inspectionPointMapper.selectList(new LambdaQueryWrapper<InspectionPoint>()
                .orderByAsc(InspectionPoint::getId));
    }

    @Override
    public InspectionPoint create(InspectionPointRequest request) {
        if (request == null || !StringUtils.hasText(request.getPointCode()) || !StringUtils.hasText(request.getLocation())) {
            throw new BizException(400, "巡检点编码和位置不能为空");
        }
        String code = request.getPointCode().trim();
        if (code.length() > 50 || request.getLocation().trim().length() > 100) {
            throw new BizException(400, "巡检点字段长度超出限制");
        }
        if (inspectionPointMapper.selectCount(new LambdaQueryWrapper<InspectionPoint>()
                .eq(InspectionPoint::getPointCode, code)) > 0) {
            throw new BizException(409, "巡检点编码已存在");
        }
        InspectionPoint point = new InspectionPoint();
        point.setPointCode(code);
        point.setLocation(request.getLocation().trim());
        inspectionPointMapper.insert(point);
        return inspectionPointMapper.selectById(point.getId());
    }

    @Override
    public byte[] qrCode(Long id) {
        InspectionPoint point = inspectionPointMapper.selectById(id);
        if (point == null) {
            throw new BizException(404, "巡检点不存在");
        }
        return QrCodeUtil.generatePng(point.getPointCode(), 300);
    }
}
