package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.dto.InspectionRecordRequest;
import com.lab.dto.InspectionRecordVO;
import com.lab.dto.InspectionSyncRequest;
import com.lab.dto.SyncResult;
import com.lab.entity.InspectionPoint;
import com.lab.entity.InspectionRecord;
import com.lab.entity.OperationLog;
import com.lab.entity.SysUser;
import com.lab.mapper.InspectionPointMapper;
import com.lab.mapper.InspectionRecordMapper;
import com.lab.mapper.OperationLogMapper;
import com.lab.mapper.SysUserMapper;
import com.lab.service.InspectionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionRecordServiceImpl implements InspectionRecordService {

    private final InspectionRecordMapper inspectionRecordMapper;
    private final InspectionPointMapper inspectionPointMapper;
    private final SysUserMapper userMapper;
    private final OperationLogMapper operationLogMapper;

    @Override
    public InspectionRecordVO createOnline(Long inspectorId, InspectionRecordRequest request) {
        InspectionPoint point = validateAndFindPoint(request);
        InspectionRecord record = insertRecord(point.getId(), inspectorId, request, false, LocalDateTime.now());
        return toVo(record, Map.of(point.getId(), point), Map.of(inspectorId, userMapper.selectById(inspectorId)));
    }

    @Override
    public SyncResult syncOffline(Long inspectorId, InspectionSyncRequest request) {
        if (request == null || request.getRecords() == null || request.getRecords().isEmpty()) {
            return new SyncResult(0, 0);
        }
        int success = 0;
        int failed = 0;
        // DEMO-SIMPLIFIED: 按契约仅返回成功/失败条数，前端无法定位具体失败项；单条失败不阻断其余记录。
        for (InspectionRecordRequest item : request.getRecords()) {
            try {
                if (item == null) {
                    throw new BizException(400, "巡检记录不能为空");
                }
                InspectionPoint point = validateAndFindPoint(item);
                if (item.getInspectedAt() == null) {
                    throw new BizException(400, "离线巡检时间不能为空");
                }
                insertRecord(point.getId(), inspectorId, item, true, item.getInspectedAt());
                writeSyncLog(item.getPointCode(), inspectorId);
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("离线巡检记录同步失败，pointCode={}，原因={}", item == null ? null : item.getPointCode(), e.getMessage());
            }
        }
        return new SyncResult(success, failed);
    }

    @Override
    public PageResult<InspectionRecordVO> page(long page, long size, Long pointId) {
        Page<InspectionRecord> pageData = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 1000));
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<InspectionRecord>()
                .eq(pointId != null, InspectionRecord::getPointId, pointId)
                .orderByDesc(InspectionRecord::getInspectedAt)
                .orderByDesc(InspectionRecord::getId);
        Page<InspectionRecord> result = inspectionRecordMapper.selectPage(pageData, wrapper);

        Set<Long> pointIds = result.getRecords().stream().map(InspectionRecord::getPointId).collect(Collectors.toSet());
        Map<Long, InspectionPoint> pointMap = pointIds.isEmpty() ? Map.of()
                : inspectionPointMapper.selectBatchIds(pointIds).stream()
                .collect(Collectors.toMap(InspectionPoint::getId, Function.identity()));
        Set<Long> userIds = result.getRecords().stream().map(InspectionRecord::getInspectorId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<InspectionRecordVO> records = result.getRecords().stream()
                .map(record -> toVo(record, pointMap, userMap))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private InspectionPoint validateAndFindPoint(InspectionRecordRequest request) {
        if (request == null || !StringUtils.hasText(request.getPointCode())) {
            throw new BizException(400, "巡检点编码不能为空");
        }
        if (!"NORMAL".equals(request.getResult()) && !"ABNORMAL".equals(request.getResult())) {
            throw new BizException(400, "巡检结果必须为 NORMAL 或 ABNORMAL");
        }
        if (request.getRemark() != null && request.getRemark().length() > 255) {
            throw new BizException(400, "巡检备注不能超过 255 字");
        }
        InspectionPoint point = inspectionPointMapper.selectOne(new LambdaQueryWrapper<InspectionPoint>()
                .eq(InspectionPoint::getPointCode, request.getPointCode().trim()));
        if (point == null) {
            throw new BizException(404, "巡检点不存在");
        }
        return point;
    }

    private InspectionRecord insertRecord(Long pointId, Long inspectorId, InspectionRecordRequest request,
                                          boolean offline, LocalDateTime inspectedAt) {
        InspectionRecord record = new InspectionRecord();
        record.setPointId(pointId);
        record.setInspectorId(inspectorId);
        record.setResult(request.getResult());
        record.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        record.setInspectedAt(inspectedAt);
        record.setSyncedFromOffline(offline ? 1 : 0);
        inspectionRecordMapper.insert(record);
        return inspectionRecordMapper.selectById(record.getId());
    }

    private void writeSyncLog(String pointCode, Long inspectorId) {
        OperationLog log = new OperationLog();
        log.setBizType("INSPECTION");
        log.setAction("SYNC_OFFLINE");
        log.setDetail("离线同步巡检点 " + pointCode);
        log.setOperatorId(inspectorId);
        operationLogMapper.insert(log);
    }

    private InspectionRecordVO toVo(InspectionRecord record, Map<Long, InspectionPoint> pointMap,
                                    Map<Long, SysUser> userMap) {
        InspectionRecordVO vo = new InspectionRecordVO();
        vo.setId(record.getId());
        vo.setPointId(record.getPointId());
        InspectionPoint point = pointMap.get(record.getPointId());
        if (point != null) {
            vo.setPointCode(point.getPointCode());
            vo.setLocation(point.getLocation());
        }
        vo.setInspectorId(record.getInspectorId());
        SysUser inspector = userMap.get(record.getInspectorId());
        if (inspector != null) {
            vo.setInspectorName(inspector.getRealName());
        }
        vo.setResult(record.getResult());
        vo.setRemark(record.getRemark());
        vo.setInspectedAt(record.getInspectedAt());
        vo.setSyncedFromOffline(record.getSyncedFromOffline());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
