package com.demo.app.api.controller;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.*;
import com.demo.app.persistence.repository.*;
import com.demo.app.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("BenefitController")
class BenefitControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private MemberTierRepository memberTierRepository;
    @Autowired private BenefitPackageRepository benefitPackageRepository;
    @Autowired private BenefitItemRepository benefitItemRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;

    private String adminToken;
    private String memberToken;

    @BeforeEach void setUp() {
        UserEntity admin = userRepository.save(TestFixtures.user("ben_admin", Role.ADMINISTRATOR));
        UserEntity member = userRepository.save(TestFixtures.user("ben_member", Role.MEMBER));
        adminToken = jwtService.generateToken(admin.getUsername(), admin.getRole().name());
        memberToken = jwtService.generateToken(member.getUsername(), member.getRole().name());

        MemberTierEntity tier = memberTierRepository.save(TestFixtures.tier("Gold", 3, 1500, null));
        benefitPackageRepository.save(BenefitPackageEntity.builder()
                .tierId(tier.getId()).name("Gold").description("").active(true).createdAt(LocalDateTime.now()).build());
        memberProfileRepository.save(TestFixtures.profile(member.getId(), tier.getId(), 2000));
    }

    @Test void testGetPackagesByTier() throws Exception {
        Long tierId = memberTierRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/benefits/packages/tier/" + tierId)
                .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }

    @Test void testGetItemsByPackage() throws Exception {
        Long pkgId = benefitPackageRepository.findAll().get(0).getId();
        BenefitItemEntity item = benefitItemRepository.save(BenefitItemEntity.builder()
                .packageId(pkgId).benefitType("DISCOUNT").benefitValue("10").scope("ORDER")
                .createdAt(LocalDateTime.now()).build());
        mockMvc.perform(get("/api/benefits/items/package/" + pkgId)
                .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }

    @Test void testIssueRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/benefits/issue")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":1,\"benefitItemId\":1,\"reference\":\"t\",\"referenceType\":\"ORDER\",\"referenceId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test void testRedeemByMember() throws Exception {
        Long pkgId = benefitPackageRepository.findAll().get(0).getId();
        BenefitItemEntity item = benefitItemRepository.save(BenefitItemEntity.builder()
                .packageId(pkgId).benefitType("DISCOUNT").benefitValue("5").scope("ORDER")
                .createdAt(LocalDateTime.now()).build());
        UserEntity member = userRepository.findByUsername("ben_member").get();
        CategoryEntity cat = categoryRepository.save(TestFixtures.category("BenCat"));
        ProductEntity prod = productRepository.save(TestFixtures.product("BenProd", BigDecimal.TEN, cat, member));
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .buyer(member).product(prod).quantity(1).totalPrice(BigDecimal.TEN)
                .status(com.demo.app.domain.enums.OrderStatus.PLACED)
                .tenderType("INTERNAL_CREDIT").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        mockMvc.perform(post("/api/benefits/redeem")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"benefitItemId\":" + item.getId() + ",\"reference\":\"t\",\"referenceType\":\"ORDER\",\"referenceId\":" + order.getId() + "}"))
                .andExpect(status().isOk());
    }
}
