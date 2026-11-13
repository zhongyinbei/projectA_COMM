package com.lab.service;

import com.lab.entity.Consumable;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> summary();

    List<Map<String, Object>> equipmentStatus();

    List<Map<String, Object>> inspectionRate();

    List<Map<String, Object>> consumableTop();

    List<Consumable> alertConsumables();
}
