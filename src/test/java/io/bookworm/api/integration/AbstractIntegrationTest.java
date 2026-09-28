package io.bookworm.api.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

/**
 * Abstract base class for all integration tests.
 * <p>
 * Why: Owns the single shared PostgreSQL Testcontainer for the entire test suite (all subclasses
 * reuse the same container via static field). Wires RestAssured's base URI on each test so
 * the random {@code RANDOM_PORT} is always reflected correctly.
 * <p>
 * Side effects: The container is started once per JVM (Testcontainers lifecycle = class-static).
 * Flyway runs all migrations automatically on first start, matching production schema exactly.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookworm_it")
            .withUsername("it_user")
            .withPassword("it_pass");

    @LocalServerPort
    private int port;

    /**
     * Binds the Spring datasource to the Testcontainer's dynamic JDBC URL before the
     * application context is loaded.
     */
    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.flyway.enabled",            () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto",    () -> "validate");
    }

    /**
     * Re-points RestAssured at the random port assigned to this test's embedded server.
     * Why: port is only available after the Spring context starts — cannot be set statically.
     */
    @BeforeEach
    void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/v1";
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    /**
     * Registers a new member and returns the full registration response body.
     * Why: used across Auth, Cart, and Order tests to bootstrap a fresh member identity.
     */
    protected Response registerMember(String email, String password, String firstName, String lastName) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", password,
                        "firstName", firstName,
                        "lastName", lastName
                ))
                .post("/auth/register");
    }

    /**
     * Logs in with the given credentials and returns the full token response.
     * Why: provides a re-usable login step so individual test cases can focus on their own assertions.
     */
    protected Response login(String email, String password) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("identifier", email, "password", password))
                .post("/auth/login");
    }

    /**
     * Registers a member and immediately returns the Bearer access token.
     * Why: most test cases only need a ready-to-use token, not the full registration body.
     */
    protected String registerAndGetToken(String email, String password) {
        registerMember(email, password, "Test", "User");
        return login(email, password)
                .then().extract().path("accessToken");
    }

    /**
     * Performs an authenticated GET request with a Bearer token.
     */
    protected Response authGet(String path, String token) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .get(path);
    }

    /**
     * Performs an authenticated POST request with a Bearer token and JSON body.
     */
    protected Response authPost(String path, String token, Object body) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(body)
                .post(path);
    }
}
