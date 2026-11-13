package com.lab.config;

import com.lab.entity.Consumable;
import com.lab.entity.Equipment;
import com.lab.entity.InspectionPoint;
import com.lab.entity.SysUser;
import com.lab.mapper.ConsumableMapper;
import com.lab.mapper.EquipmentMapper;
import com.lab.mapper.InspectionPointMapper;
import com.lab.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final EquipmentMapper equipmentMapper;
    private final InspectionPointMapper inspectionPointMapper;
    private final ConsumableMapper consumableMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            log.info("检测到已有种子数据，跳过初始化");
            return;
        }

        userMapper.insert(user("admin", "admin123", "系统管理员", "ADMIN", "13800000001"));
        userMapper.insert(user("repairer1", "123456", "维修员小李", "REPAIRER", "13800000002"));
        userMapper.insert(user("reporter1", "123456", "报修员小王", "REPORTER", "13800000003"));

        equipmentMapper.insert(equipment("LAB-PC-001", "教学投影仪", "A301 多媒体教室", "显示设备", "NORMAL"));
        equipmentMapper.insert(equipment("LAB-PC-002", "实训电脑", "A302 机房 01 号机", "计算机", "NORMAL"));
        equipmentMapper.insert(equipment("LAB-NET-003", "网络交换机", "A302 机房弱电柜", "网络设备", "REPAIRING"));
        equipmentMapper.insert(equipment("LAB-3D-004", "3D 打印机", "B201 创客实训室", "加工设备", "NORMAL"));
        equipmentMapper.insert(equipment("LAB-OSC-005", "数字示波器", "B202 电子实训室", "测量仪器", "SCRAPPED"));

        inspectionPointMapper.insert(point("P-001", "A301 多媒体教室"));
        inspectionPointMapper.insert(point("P-002", "A302 机房弱电柜"));
        inspectionPointMapper.insert(point("P-003", "B201 创客实训室"));

        consumableMapper.insert(consumable("A4 打印纸", "80g / 500 张", "包", 50, 10));
        consumableMapper.insert(consumable("网线水晶头", "RJ45 六类", "盒", 20, 5));
        consumableMapper.insert(consumable("焊锡丝", "0.8mm / 100g", "卷", 15, 5));
        consumableMapper.insert(consumable("酒精棉片", "100 片 / 盒", "盒", 30, 8));
        consumableMapper.insert(consumable("光纤跳线", "LC-LC 3m", "根", 2, 5));

        log.info("种子数据初始化完成：3 个账号、5 台设备、3 个巡检点、5 种耗材");
    }

    private SysUser user(String username, String rawPassword, String realName, String role, String phone) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setRole(role);
        user.setPhone(phone);
        return user;
    }

    private Equipment equipment(String assetNo, String name, String location, String category, String status) {
        Equipment equipment = new Equipment();
        equipment.setAssetNo(assetNo);
        equipment.setName(name);
        equipment.setLocation(location);
        equipment.setCategory(category);
        equipment.setStatus(status);
        return equipment;
    }

    private InspectionPoint point(String code, String location) {
        InspectionPoint point = new InspectionPoint();
        point.setPointCode(code);
        point.setLocation(location);
        return point;
    }

    private Consumable consumable(String name, String spec, String unit, int stock, int threshold) {
        Consumable consumable = new Consumable();
        consumable.setName(name);
        consumable.setSpec(spec);
        consumable.setUnit(unit);
        consumable.setStock(stock);
        consumable.setThreshold(threshold);
        return consumable;
    }
}
