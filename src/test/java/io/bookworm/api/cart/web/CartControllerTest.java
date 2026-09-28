package io.bookworm.api.cart.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bookworm.api.cart.application.CartService;
import io.bookworm.api.cart.dto.AddCartItemRequest;
import io.bookworm.api.cart.dto.CartResponse;
import io.bookworm.api.cart.dto.MergeCartRequest;
import io.bookworm.api.cart.dto.UpdateCartItemRequest;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
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
 * Controller unit tests for {@link CartController} using MockMvc, Mockito, and AssertJ.
 * <p>
 * Why: Verifies guest/member cart resolution, item management endpoints, merge logic,
 * and HTTP response status codes.
 * Side effects: Exercises Cart web endpoints in standalone MVC context with simulated security principals.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartController Web Tests")
class CartControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private UUID memberId;
    private UUID storeId;
    private UUID cartId;
    private UUID bookId;
    private UUID formatId;
    private UUID itemId;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        formatId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        userDetails = new User(memberId.toString(), "password", Collections.emptyList());

        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(UserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return userDetails;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("GET /cart - Should return 200 OK with cart details")
    void getCart_shouldReturn200() throws Exception {
        // Arrange
        CartResponse response = CartResponse.builder()
                .cartId(cartId)
                .itemCount(1)
                .subtotal(MoneyDTO.builder().amount("499.00").currency("INR").build())
                .items(List.of())
                .build();

        when(cartService.getCart(eq(memberId), any(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").isEqualTo(cartId.toString()))
                .andExpect(jsonPath("$.itemCount").isEqualTo(1));
    }

    @Test
    @DisplayName("POST /cart/items - Should return 201 Created when item added")
    void addItem_shouldReturn201() throws Exception {
        // Arrange
        AddCartItemRequest request = new AddCartItemRequest();
        request.setBookId(bookId);
        request.setBookFormatId(formatId);
        request.setQuantity(1);

        CartResponse response = CartResponse.builder()
                .cartId(cartId)
                .itemCount(1)
                .build();

        when(cartService.addItem(eq(memberId), any(), any(), any(AddCartItemRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartId").isEqualTo(cartId.toString()));
    }

    @Test
    @DisplayName("PATCH /cart/items/{itemId} - Should return 200 OK on quantity update")
    void updateItemQuantity_shouldReturn200() throws Exception {
        // Arrange
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(3);

        CartResponse response = CartResponse.builder()
                .cartId(cartId)
                .itemCount(3)
                .build();

        when(cartService.updateItemQuantity(eq(memberId), any(), any(), eq(itemId), any(UpdateCartItemRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/cart/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").isEqualTo(3));
    }

    @Test
    @DisplayName("DELETE /cart/items/{itemId} - Should return 204 No Content")
    void removeItem_shouldReturn204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/cart/items/{itemId}", itemId))
                .andExpect(status().isNoContent());

        verify(cartService).removeItem(eq(memberId), any(), eq(itemId));
    }

    @Test
    @DisplayName("POST /cart/merge - Should return 200 OK after merging guest cart")
    void mergeCart_shouldReturn200() throws Exception {
        // Arrange
        MergeCartRequest request = new MergeCartRequest();
        request.setGuestToken("guest-token-xyz");

        CartResponse response = CartResponse.builder()
                .cartId(cartId)
                .itemCount(4)
                .build();

        when(cartService.mergeCart(eq(memberId), any(), any(MergeCartRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/cart/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").isEqualTo(4));
    }
}
