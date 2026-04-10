package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.domain.model.Warehouse;
import com.demo.app.persistence.entity.WarehouseEntity;
import com.demo.app.persistence.repository.WarehouseRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("WarehouseService")
class WarehouseServiceTest {
    @Autowired private WarehouseService warehouseService;
    @Autowired private WarehouseRepository warehouseRepository;

    @BeforeEach void setUp() {
        warehouseRepository.save(WarehouseEntity.builder()
                .name("Test WH").code("WH-T").location("L1").active(true).createdAt(LocalDateTime.now()).build());
    }

    @Test void testGetAll() { assertFalse(warehouseService.getAll().isEmpty()); }
    @Test void testGetActive() { assertFalse(warehouseService.getActive().isEmpty()); }
    @Test void testGetByCode() { assertNotNull(warehouseService.getByCode("WH-T")); }
    @Test void testGetById() {
        Long id = warehouseRepository.findByCode("WH-T").get().getId();
        assertNotNull(warehouseService.getById(id));
    }
}
