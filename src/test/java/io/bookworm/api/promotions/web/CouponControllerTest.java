package io.bookworm.api.promotions.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.dto.PaginationDTO;
import io.bookworm.api.common.web.GlobalExceptionHandler;
import io.bookworm.api.promotions.application.CouponService;
import io.bookworm.api.promotions.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller unit tests for {@link CouponController} using MockMvc, Mockito, and AssertJ.
 * <p>
 * Why: Verifies HTTP routing, JSON serialization/deserialization, request validation,
 * and status code responses for coupon management and validation endpoints.
 * Side effects: Exercises MVC layer in standalone mode without loading full Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CouponController Web Tests")
class CouponControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    private UUID storeId;
    private UUID couponId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(couponController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        storeId = UUID.randomUUID();
        couponId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /coupons - Should return 200 OK with paginated coupons")
    void listCoupons_shouldReturn200() throws Exception {
        // Arrange
        CouponListResponse response = CouponListResponse.builder()
                .content(List.of(CouponDTO.builder().code("SAVE20").build()))
                .pagination(PaginationDTO.builder().currentPage(0).pageSize(20).totalElements(1).build())
                .build();

        when(couponService.getCoupons(eq(storeId), any(Pageable.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/coupons")
                        .param("storeId", storeId.toString())
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").isEqualTo("SAVE20"))
                .andExpect(jsonPath("$.pagination.totalElements").isEqualTo(1));
    }

    @Test
    @DisplayName("POST /coupons - Should return 201 Created when coupon payload is valid")
    void createCoupon_shouldReturn201() throws Exception {
        // Arrange
        CreateCouponRequest request = new CreateCouponRequest();
        request.setStoreId(storeId);
        request.setCode("WELCOME50");
        request.setDiscountType(CreateCouponRequest.DiscountTypeEnum.FLAT);
        request.setDiscountValue(50.0);

        CouponDTO createdDto = CouponDTO.builder()
                .couponId(couponId)
                .code("WELCOME50")
                .discountValue(50.0)
                .build();

        when(couponService.createCoupon(eq(storeId), any(CreateCouponRequest.class))).thenReturn(createdDto);

        // Act & Assert
        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.couponId").isEqualTo(couponId.toString()))
                .andExpect(jsonPath("$.code").isEqualTo("WELCOME50"));
    }

    @Test
    @DisplayName("POST /coupons/validate - Should return 200 OK with validation result")
    void validateCoupon_shouldReturn200() throws Exception {
        // Arrange
        ValidateCouponRequest request = new ValidateCouponRequest();
        request.setCode("SAVE20");
        request.setOrderAmount("500.00");

        CouponValidationResponse validationResponse = CouponValidationResponse.builder()
                .isValid(true)
                .couponCode("SAVE20")
                .discountType("PERCENT")
                .discountAmount(MoneyDTO.builder().amount("100.00").currency("INR").build())
                .build();

        when(couponService.validateCoupon(eq(null), any(ValidateCouponRequest.class))).thenReturn(validationResponse);

        // Act & Assert
        mockMvc.perform(post("/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").isEqualTo(true))
                .andExpect(jsonPath("$.couponCode").isEqualTo("SAVE20"))
                .andExpect(jsonPath("$.discountAmount.amount").isEqualTo("100.00"));
    }

    @Test
    @DisplayName("PUT /coupons/{couponId} - Should return 200 OK when coupon updated")
    void updateCoupon_shouldReturn200() throws Exception {
        // Arrange
        UpdateCouponRequest request = new UpdateCouponRequest();
        request.setDescription("Updated description");
        request.setVersion(1);

        CouponDTO updatedDto = CouponDTO.builder()
                .couponId(couponId)
                .description("Updated description")
                .build();

        when(couponService.updateCoupon(eq(couponId), any(UpdateCouponRequest.class))).thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(put("/coupons/{couponId}", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").isEqualTo("Updated description"));
    }

    @Test
    @DisplayName("PATCH /coupons/{couponId} - Should return 200 OK on status update")
    void updateCouponStatus_shouldReturn200() throws Exception {
        // Arrange
        CouponUpdateStatusRequest request = new CouponUpdateStatusRequest();
        request.setIsActive(false);

        // Act & Assert
        mockMvc.perform(patch("/coupons/{couponId}", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(couponService).updateCouponStatus(eq(couponId), any(CouponUpdateStatusRequest.class));
    }
}
