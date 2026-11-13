package com.lab.service;

import com.lab.dto.InspectionPointRequest;
import com.lab.entity.InspectionPoint;

import java.util.List;

public interface InspectionPointService {

    List<InspectionPoint> list();

    InspectionPoint create(InspectionPointRequest request);

    byte[] qrCode(Long id);
}
