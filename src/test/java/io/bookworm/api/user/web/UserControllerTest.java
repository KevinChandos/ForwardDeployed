package io.bookworm.api.user.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bookworm.api.common.web.GlobalExceptionHandler;
import io.bookworm.api.user.application.UserService;
import io.bookworm.api.user.application.WishlistService;
import io.bookworm.api.user.dto.AddWishlistItemRequest;
import io.bookworm.api.user.dto.WishlistResponse;
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
 * Controller unit tests for {@link UserController} wishlist endpoints using MockMvc and Mockito.
 * <p>
 * Why: Tests HTTP status codes, security principal resolution, and JSON serialization for wishlist endpoints.
 * Side effects: Exercises MVC layer in standalone mode with injected UserDetails.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Wishlist Web Tests")
class UserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private UserController userController;

    private UUID memberId;
    private UUID wishlistId;
    private UUID bookId;
    private UUID formatId;
    private UUID itemId;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        wishlistId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        formatId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        userDetails = new User(memberId.toString(), "password", Collections.emptyList());

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
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
    @DisplayName("GET /users/me/wishlist - Should return 200 OK with wishlist")
    void getMyWishlist_shouldReturn200() throws Exception {
        // Arrange
        WishlistResponse response = WishlistResponse.builder()
                .wishlistId(wishlistId)
                .itemCount(0)
                .items(List.of())
                .build();

        when(wishlistService.getWishlist(memberId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/users/me/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wishlistId").isEqualTo(wishlistId.toString()))
                .andExpect(jsonPath("$.itemCount").isEqualTo(0));
    }

    @Test
    @DisplayName("POST /users/me/wishlist/items - Should return 200 OK when item added")
    void addWishlistItem_shouldReturn200() throws Exception {
        // Arrange
        AddWishlistItemRequest request = new AddWishlistItemRequest();
        request.setBookId(bookId);
        request.setBookFormatId(formatId);

        WishlistResponse response = WishlistResponse.builder()
                .wishlistId(wishlistId)
                .itemCount(1)
                .build();

        when(wishlistService.addWishlistItem(eq(memberId), any(AddWishlistItemRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/users/me/wishlist/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wishlistId").isEqualTo(wishlistId.toString()))
                .andExpect(jsonPath("$.itemCount").isEqualTo(1));
    }

    @Test
    @DisplayName("DELETE /users/me/wishlist/items/{wishlistItemId} - Should return 204 No Content")
    void removeWishlistItem_shouldReturn204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/users/me/wishlist/items/{wishlistItemId}", itemId))
                .andExpect(status().isNoContent());

        verify(wishlistService).removeWishlistItem(memberId, itemId);
    }
}
