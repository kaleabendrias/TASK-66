package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.User;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("UserService")
class UserServiceTest {
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;

    @BeforeEach void setUp() {
        userRepository.save(TestFixtures.user("usvc1", Role.MEMBER));
        userRepository.save(TestFixtures.user("usvc2", Role.SELLER));
    }

    @Test void testGetAll() { assertTrue(userService.getAll().size() >= 2); }
    @Test void testGetByUsername() { assertNotNull(userService.getByUsername("usvc1")); }
    @Test void testGetById() {
        Long id = userRepository.findByUsername("usvc1").get().getId();
        assertNotNull(userService.getById(id));
    }
    @Test void testDeleteUser() {
        Long id = userRepository.findByUsername("usvc2").get().getId();
        userService.deleteUser(id);
        assertFalse(userRepository.existsById(id));
    }
}
