package io.bookworm.api.config;

import io.bookworm.api.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA configuration class.
 * <p>
 * Why: {@code @EnableJpaAuditing} is declared on the application class, but this
 * bean provides the {@code AuditorAware} implementation that resolves the current
 * actor UUID from the security context. Keeping it here (not in BookwormApplication)
 * prevents Mockito/Spring from picking it up in slice tests where Security is absent.
 * <p>
 * Side effects: if no authenticated principal is present (e.g. during scheduled
 * jobs or outbox relay), {@code createdBy} / {@code updatedBy} will be empty,
 * resulting in a NULL column value — which is acceptable per the schema design.
 */
@Configuration
public class JpaConfig {

    /**
     * Supplies the current member UUID to Spring Data auditing.
     * Returns {@link Optional#empty()} for unauthenticated requests,
     * which writes NULL into created_by / updated_by.
     */
    @Bean
    public AuditorAware<UUID> springSecurityAuditorAware() {
        return SecurityUtils::getCurrentMemberId;
    }
}
