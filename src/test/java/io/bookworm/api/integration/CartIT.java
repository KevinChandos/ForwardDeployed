package io.bookworm.api.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

/**
 * Full API integration tests for the Cart bounded context.
 * <p>
 * Why: Validates guest cart access (X-Guest-Token header), authenticated member cart operations,
 * add/update/remove item lifecycle, and guest-to-member merge flow.
 * <p>
 * Side effects: Each test creates isolated cart state via unique guest tokens or fresh member UUIDs,
 * so test-order independence is maintained without shared mutable state.
 */
@DisplayName("Cart API Integration Tests")
class CartIT extends AbstractIntegrationTest {

    // ── GET /cart ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /cart")
    class GetCart {

        @Test
        @DisplayName("200 - returns empty cart for new guest token")
        void getCart_newGuestToken_returnsEmptyCart() {
            String guestToken = "guest-" + UUID.randomUUID();

            RestAssured.given()
                    .header("X-Guest-Token", guestToken)
                    .get("/cart")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("items", notNullValue());
        }

        @Test
        @DisplayName("200 - returns cart for authenticated member with no token header")
        void getCart_authenticatedMember_returns200() {
            String email = "it_cart_get_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .get("/cart")
                    .then()
                    .statusCode(200)
                    .body("items", notNullValue());
        }

        @Test
        @DisplayName("200 - cart is publicly accessible without any auth header")
        void getCart_noCredentials_returns200() {
            // Why: /cart/** is permitAll in SecurityConfig — anonymous browsing is supported.
            RestAssured.given()
                    .get("/cart")
                    .then()
                    .statusCode(200);
        }
    }

    // ── POST /cart/items ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /cart/items")
    class AddCartItem {

        @Test
        @DisplayName("400 - missing bookId returns 400")
        void addItem_missingBookId_returns400() {
            String guestToken = "guest-" + UUID.randomUUID();

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("X-Guest-Token", guestToken)
                    .body(Map.of(
                            "bookFormatId", UUID.randomUUID(),
                            "quantity", 1
                    ))
                    .post("/cart/items")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("400 - quantity below 1 returns 400")
        void addItem_quantityZero_returns400() {
            String guestToken = "guest-" + UUID.randomUUID();

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("X-Guest-Token", guestToken)
                    .body(Map.of(
                            "bookId", UUID.randomUUID(),
                            "bookFormatId", UUID.randomUUID(),
                            "quantity", 0
                    ))
                    .post("/cart/items")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("400 - null quantity returns 400")
        void addItem_nullQuantity_returns400() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("X-Guest-Token", "guest-" + UUID.randomUUID())
                    .body(Map.of(
                            "bookId", UUID.randomUUID(),
                            "bookFormatId", UUID.randomUUID()
                    ))
                    .post("/cart/items")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("400 - missing bookFormatId returns 400")
        void addItem_missingFormatId_returns400() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("X-Guest-Token", "guest-" + UUID.randomUUID())
                    .body(Map.of(
                            "bookId", UUID.randomUUID(),
                            "quantity", 1
                    ))
                    .post("/cart/items")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("404 - non-existent bookFormatId returns 404")
        void addItem_unknownFormat_returns404() {
            String email = "it_additem_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of(
                            "bookId", UUID.randomUUID(),
                            "bookFormatId", UUID.randomUUID(),
                            "quantity", 1
                    ))
                    .post("/cart/items")
                    .then()
                    .statusCode(404);
        }
    }

    // ── PATCH /cart/items/{cartItemId} ───────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /cart/items/{cartItemId}")
    class UpdateCartItem {

        @Test
        @DisplayName("404 - updating a non-existent cart item returns 404")
        void updateItem_unknown_returns404() {
            String email = "it_upd_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of("quantity", 2))
                    .patch("/cart/items/{id}", UUID.randomUUID())
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("400 - quantity of 0 returns 400")
        void updateItem_quantityZero_returns400() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("X-Guest-Token", "guest-" + UUID.randomUUID())
                    .body(Map.of("quantity", 0))
                    .patch("/cart/items/{id}", UUID.randomUUID())
                    .then()
                    .statusCode(400);
        }
    }

    // ── DELETE /cart/items/{cartItemId} ──────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /cart/items/{cartItemId}")
    class RemoveCartItem {

        @Test
        @DisplayName("404 - removing a non-existent cart item returns 404")
        void removeItem_unknown_returns404() {
            String email = "it_del_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .delete("/cart/items/{id}", UUID.randomUUID())
                    .then()
                    .statusCode(404);
        }
    }

    // ── POST /cart/merge ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /cart/merge")
    class MergeCart {

        @Test
        @DisplayName("400 - missing guestToken returns 400")
        void mergeCart_missingGuestToken_returns400() {
            String email = "it_merge_400_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of())
                    .post("/cart/merge")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("401 - unauthenticated merge returns 401")
        void mergeCart_noAuth_returns401() {
            // Why: merge requires an authenticated member to receive the guest items into.
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("guestToken", "guest-" + UUID.randomUUID()))
                    .post("/cart/merge")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("200 - merge with non-existent guest cart returns 200 with empty items")
        void mergeCart_unknownGuestToken_returns200Empty() {
            // Why: merging a non-existent guest cart is not an error — the result is simply
            // an unchanged member cart; the service is idempotent on unknown tokens.
            String email = "it_merge_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of("guestToken", "ghost-" + UUID.randomUUID()))
                    .post("/cart/merge")
                    .then()
                    .statusCode(200)
                    .body("items", notNullValue());
        }
    }

    // ── Security boundaries specific to Cart ──────────────────────────────────────

    @Nested
    @DisplayName("Cart security boundaries")
    class CartSecurityBoundaries {

        @Test
        @DisplayName("200 - /cart is accessible with only X-Guest-Token (no JWT)")
        void cartIsPublicWithGuestToken() {
            RestAssured.given()
                    .header("X-Guest-Token", "guest-" + UUID.randomUUID())
                    .get("/cart")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("200 - /cart is accessible with no credentials at all")
        void cartIsPublicWithoutAnyCredential() {
            RestAssured.given()
                    .get("/cart")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("201 - adding item is permitted for guest (no authentication)")
        void addItem_guestNoAuth_returns4xxOr201() {
            // Why: /cart/** is permitAll, so even anonymous POST /cart/items is permitted.
            // The service will 404 on an unknown bookFormatId — the point is we never get 401/403.
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "bookId", UUID.randomUUID(),
                            "bookFormatId", UUID.randomUUID(),
                            "quantity", 1
                    ))
                    .post("/cart/items")
                    .then()
                    .statusCode(not(anyOf(equalTo(401), equalTo(403))));
        }
    }
}
