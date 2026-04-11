package com.demo.app.api.controller;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.CategoryEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.CategoryRepository;
import com.demo.app.persistence.repository.ProductRepository;
import com.demo.app.persistence.repository.UserRepository;
import com.demo.app.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ListingController - listing creation with discovery fields")
class ListingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;

    private String sellerToken;
    private String otherSellerToken;
    private ProductEntity product;

    @BeforeEach
    void setUp() {
        UserEntity seller = userRepository.save(TestFixtures.user("listctl_seller", Role.SELLER));
        UserEntity otherSeller = userRepository.save(TestFixtures.user("listctl_other", Role.SELLER));
        sellerToken = jwtService.generateToken(seller.getUsername(), seller.getRole().name());
        otherSellerToken = jwtService.generateToken(otherSeller.getUsername(), otherSeller.getRole().name());

        CategoryEntity category = categoryRepository.save(TestFixtures.category("Lofts"));
        product = productRepository.save(
                TestFixtures.product("Loft listing", new BigDecimal("1500.00"), category, seller));
    }

    private Map<String, Object> body(String slug) {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", product.getId());
        body.put("title", "Sunlit downtown loft");
        body.put("slug", slug);
        body.put("summary", "Two-bedroom loft with high ceilings");
        body.put("tags", new String[]{"loft", "downtown"});
        body.put("featured", false);
        body.put("neighborhood", "Arts District");
        body.put("latitude", 40.7128);
        body.put("longitude", -74.0060);
        body.put("price", 2400.00);
        body.put("sqft", 950);
        body.put("layout", "2BR/1BA");
        body.put("availableFrom", "2026-05-01");
        body.put("availableTo", "2026-12-31");
        return body;
    }

    @Test
    @DisplayName("POST /api/listings persists discovery fields and returns them in the response")
    void create_persistsDiscoveryFields() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("listing-discovery-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.neighborhood").value("Arts District"))
                .andExpect(jsonPath("$.latitude").value(40.7128))
                .andExpect(jsonPath("$.longitude").value(-74.0060))
                .andExpect(jsonPath("$.price").value(2400.00))
                .andExpect(jsonPath("$.sqft").value(950))
                .andExpect(jsonPath("$.layout").value("2BR/1BA"))
                .andExpect(jsonPath("$.availableFrom").value("2026-05-01"))
                .andExpect(jsonPath("$.availableTo").value("2026-12-31"));
    }

    @Test
    @DisplayName("POST /api/listings rejects sellers attempting to list someone else's product")
    void create_otherSellersProduct_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + otherSellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("listing-discovery-2"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/listings is publicly accessible")
    void list_publicAccess() throws Exception {
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk());
    }
}
