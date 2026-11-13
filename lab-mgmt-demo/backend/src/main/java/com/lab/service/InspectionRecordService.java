package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.InspectionRecordRequest;
import com.lab.dto.InspectionRecordVO;
import com.lab.dto.InspectionSyncRequest;
import com.lab.dto.SyncResult;

public interface InspectionRecordService {

    InspectionRecordVO createOnline(Long inspectorId, InspectionRecordRequest request);

    SyncResult syncOffline(Long inspectorId, InspectionSyncRequest request);

    PageResult<InspectionRecordVO> page(long page, long size, Long pointId);
}
