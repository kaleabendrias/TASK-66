package com.demo.app.security;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("RBAC Integration - Role-based access control for endpoints")
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String memberToken;
    private String sellerToken;
    private String warehouseToken;
    private String moderatorToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        UserEntity member = userRepository.save(TestFixtures.user("member_user", Role.MEMBER));
        UserEntity seller = userRepository.save(TestFixtures.user("seller_user", Role.SELLER));
        UserEntity warehouse = userRepository.save(TestFixtures.user("warehouse_user", Role.WAREHOUSE_STAFF));
        UserEntity moderator = userRepository.save(TestFixtures.user("moderator_user", Role.MODERATOR));
        UserEntity admin = userRepository.save(TestFixtures.user("admin_user", Role.ADMINISTRATOR));

        memberToken = jwtService.generateToken(member.getUsername(), member.getRole().name());
        sellerToken = jwtService.generateToken(seller.getUsername(), seller.getRole().name());
        warehouseToken = jwtService.generateToken(warehouse.getUsername(), warehouse.getRole().name());
        moderatorToken = jwtService.generateToken(moderator.getUsername(), moderator.getRole().name());
        adminToken = jwtService.generateToken(admin.getUsername(), admin.getRole().name());
    }

    @Test
    @DisplayName("Guest can access public endpoints without authentication")
    void testGuestCanAccessPublicEndpoints() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        // /api/tiers is also public
        mockMvc.perform(get("/api/tiers"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated user cannot access protected endpoints")
    void testUnauthenticatedCannotAccessProtectedEndpoints() throws Exception {
        // Spring Security returns 403 for unauthenticated requests to protected endpoints
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Member cannot list all orders (requires staff role)")
    void testMemberCannotListAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Member can access authenticated endpoints")
    void testMemberCanAccessAuthenticated() throws Exception {
        mockMvc.perform(get("/api/tiers")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Member cannot access admin user list")
    void testMemberCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Member cannot access risk endpoints")
    void testMemberCannotAccessRisk() throws Exception {
        mockMvc.perform(get("/api/risk/high-risk")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Warehouse staff can access warehouse and order endpoints")
    void testWarehouseStaffCanAccessInventory() throws Exception {
        mockMvc.perform(get("/api/warehouses")
                        .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Seller can READ warehouses (needed to scope inventory mutations to a location)")
    void testSellerCanReadWarehouses() throws Exception {
        mockMvc.perform(get("/api/warehouses")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Moderator can access incidents and appeals")
    void testModeratorCanAccessIncidentsAndAppeals() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/appeals")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin can access all protected endpoints")
    void testAdminCanAccessEverything() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/warehouses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/risk/high-risk")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
