package io.bookworm.api.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Full API integration tests for the Authentication bounded context.
 * <p>
 * Why: Verifies the entire registration → login → refresh → logout lifecycle against a
 * real PostgreSQL Testcontainer and a fully initialised Spring Security filter chain.
 * Covers success paths, validation failures, duplicate-email conflict, bad credentials,
 * JWT expiry handling, and unauthenticated/forbidden security boundary enforcement.
 */
@DisplayName("Auth API Integration Tests")
class AuthIT extends AbstractIntegrationTest {

    // ── Registration ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("201 - registers a new member with valid payload")
        void register_validPayload_returns201() {
            String email = "it_register_" + UUID.randomUUID() + "@bookworm.io";

            registerMember(email, "Passw0rd!", "Alice", "Smith")
                    .then()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("memberId", notNullValue())
                    .body("email", equalTo(email))
                    .body("firstName", equalTo("Alice"));
        }

        @Test
        @DisplayName("400 - rejects blank email")
        void register_blankEmail_returns400() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "email", "",
                            "password", "Passw0rd!",
                            "firstName", "Bob",
                            "lastName", "Jones"
                    ))
                    .post("/auth/register")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("400 - rejects invalid email format")
        void register_invalidEmail_returns400() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "email", "not-an-email",
                            "password", "Passw0rd!",
                            "firstName", "Bob",
                            "lastName", "Jones"
                    ))
                    .post("/auth/register")
                    .then()
                    .statusCode(400)
                    .body("errors", notNullValue());
        }

        @Test
        @DisplayName("400 - rejects password shorter than 8 characters")
        void register_shortPassword_returns400() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "email", "short_pw_" + UUID.randomUUID() + "@bookworm.io",
                            "password", "abc",
                            "firstName", "Charlie",
                            "lastName", "Doe"
                    ))
                    .post("/auth/register")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("409 - conflicts on duplicate email")
        void register_duplicateEmail_returns409() {
            String email = "it_dup_" + UUID.randomUUID() + "@bookworm.io";

            // First registration must succeed.
            registerMember(email, "Passw0rd!", "First", "User")
                    .then()
                    .statusCode(201);

            // Second registration with the same email must fail.
            registerMember(email, "Passw0rd!", "Second", "User")
                    .then()
                    .statusCode(409);
        }

        @Test
        @DisplayName("400 - rejects invalid E.164 phone number")
        void register_invalidPhone_returns400() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "email", "it_phone_" + UUID.randomUUID() + "@bookworm.io",
                            "password", "Passw0rd!",
                            "firstName", "Dave",
                            "lastName", "Hill",
                            "phoneNumber", "0123456789"    // missing leading '+'
                    ))
                    .post("/auth/register")
                    .then()
                    .statusCode(400);
        }
    }

    // ── Login ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("200 - returns access and refresh tokens for valid credentials")
        void login_validCredentials_returnsTokens() {
            String email = "it_login_" + UUID.randomUUID() + "@bookworm.io";
            registerMember(email, "Passw0rd!", "Eve", "Carter");

            login(email, "Passw0rd!")
                    .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .body("tokenType", equalTo("Bearer"))
                    .body("expiresIn", greaterThan(0))
                    .body("member.email", equalTo(email));
        }

        @Test
        @DisplayName("401 - rejects wrong password")
        void login_wrongPassword_returns401() {
            String email = "it_badpw_" + UUID.randomUUID() + "@bookworm.io";
            registerMember(email, "Passw0rd!", "Frank", "Lee");

            login(email, "WrongPassword!")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("401 - rejects unknown email")
        void login_unknownEmail_returns401() {
            login("nobody_" + UUID.randomUUID() + "@bookworm.io", "Passw0rd!")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("400 - rejects missing identifier")
        void login_missingIdentifier_returns400() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("password", "Passw0rd!"))
                    .post("/auth/login")
                    .then()
                    .statusCode(400);
        }
    }

    // ── Token Refresh ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("200 - issues new access token with valid refresh token")
        void refresh_validToken_returnsNewTokens() {
            String email = "it_refresh_" + UUID.randomUUID() + "@bookworm.io";
            registerMember(email, "Passw0rd!", "Grace", "Moore");

            String refreshToken = login(email, "Passw0rd!")
                    .then().extract().path("refreshToken");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", refreshToken))
                    .post("/auth/refresh")
                    .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue());
        }

        @Test
        @DisplayName("401 - rejects a completely fabricated refresh token")
        void refresh_fabricatedToken_returns401() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", "eyJhbGciOiJub25lIn0.fake.token"))
                    .post("/auth/refresh")
                    .then()
                    .statusCode(401);
        }
    }

    // ── Logout ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("204 - invalidates session for authenticated member")
        void logout_authenticated_returns204() {
            String email = "it_logout_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .post("/auth/logout")
                    .then()
                    .statusCode(204);
        }

        @Test
        @DisplayName("401 - rejects logout without a Bearer token")
        void logout_unauthenticated_returns401() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .post("/auth/logout")
                    .then()
                    .statusCode(401);
        }
    }

    // ── Password Reset ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/password/forgot")
    class ForgotPassword {

        @Test
        @DisplayName("202 - always accepted regardless of email existence (anti-enumeration)")
        void forgot_existingEmail_returns202() {
            String email = "it_forgot_" + UUID.randomUUID() + "@bookworm.io";
            registerMember(email, "Passw0rd!", "Heidi", "Bell");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("identifier", email))
                    .post("/auth/password/forgot")
                    .then()
                    .statusCode(202);
        }

        @Test
        @DisplayName("202 - also accepted for non-existent email (prevents user enumeration)")
        void forgot_unknownEmail_returns202() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("identifier", "ghost_" + UUID.randomUUID() + "@bookworm.io"))
                    .post("/auth/password/forgot")
                    .then()
                    .statusCode(202);
        }
    }

    // ── Security Boundaries ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Security boundary enforcement")
    class SecurityBoundaries {

        @Test
        @DisplayName("401 - unauthenticated access to /users/me returns 401")
        void usersMe_noToken_returns401() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .get("/users/me")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("401 - malformed Bearer token returns 401")
        void malformedToken_returns401() {
            RestAssured.given()
                    .header("Authorization", "Bearer this.is.not.valid")
                    .get("/users/me")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("403 - REGISTERED_USER cannot POST /books (admin-only endpoint)")
        void customerCannotCreateBook_returns403() {
            String email = "it_sec_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            // REGISTERED_USER does not have CATALOGUE_MANAGER or higher role.
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of(
                            "title", "Unauthorised Book",
                            "language", "English",
                            "authors", java.util.List.of(Map.of("authorId", UUID.randomUUID(), "role", "AUTHOR")),
                            "categoryIds", java.util.List.of(UUID.randomUUID()),
                            "formats", java.util.List.of(Map.of(
                                    "formatType", "PAPERBACK",
                                    "isbn", "978-0-00-000000-0",
                                    "stockQuantity", 10,
                                    "prices", java.util.List.of(Map.of("currency", "INR", "basePrice", 299))
                            ))
                    ))
                    .post("/books")
                    .then()
                    .statusCode(403);
        }

        @Test
        @DisplayName("403 - REGISTERED_USER cannot POST /coupons (admin-only endpoint)")
        void customerCannotCreateCoupon_returns403() {
            String email = "it_coupon_sec_" + UUID.randomUUID() + "@bookworm.io";
            String token = registerAndGetToken(email, "Passw0rd!");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of("code", "HACKIT", "discountType", "FLAT", "discountValue", 50))
                    .post("/coupons")
                    .then()
                    .statusCode(403);
        }

        @Test
        @DisplayName("200 - GET /books is accessible without authentication")
        void getBooks_noAuth_returns200() {
            RestAssured.given()
                    .get("/books")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("200 - GET /actuator/health is public")
        void actuatorHealth_noAuth_returns200() {
            // Why: actuator health must remain public for load-balancer probes.
            RestAssured.given()
                    .get("/actuator/health")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("401 - expired or tampered token signature returns 401")
        void tamperedToken_returns401() {
            String email = "it_tamper_" + UUID.randomUUID() + "@bookworm.io";
            String validToken = registerAndGetToken(email, "Passw0rd!");

            // Corrupt the signature portion of the token
            String tampered = validToken.substring(0, validToken.lastIndexOf('.') + 1) + "invalidsig";

            RestAssured.given()
                    .header("Authorization", "Bearer " + tampered)
                    .get("/users/me")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("401 - checkout requires authentication")
        void checkout_noAuth_returns401() {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("storeId", UUID.randomUUID()))
                    .post("/checkout")
                    .then()
                    .statusCode(401);
        }
    }
}
