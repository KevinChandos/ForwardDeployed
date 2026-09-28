package io.bookworm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Book Worm E-Store API application.
 * {@code @EnableJpaAuditing} activates Spring Data's AuditingEntityListener,
 * which populates {@code createdAt} and {@code updatedAt} on every save.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class BookwormApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookwormApplication.class, args);
    }
}
