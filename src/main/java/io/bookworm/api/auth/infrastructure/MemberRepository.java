package io.bookworm.api.auth.infrastructure;

import io.bookworm.api.auth.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Member} entities.
 * <p>
 * Why: custom finders use JPQL with {@code WHERE deletedAt IS NULL} to exclude
 * soft-deleted rows from all standard lookups. Raw Spring Data derived queries
 * (findByEmail) do not include the soft-delete filter automatically, hence the
 * explicit {@code @Query} annotations.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    /**
     * Finds an active member by normalised lowercase email.
     * Used during login and registration duplicate checks.
     */
    @Query("SELECT m FROM Member m WHERE m.email = :email AND m.deletedAt IS NULL")
    Optional<Member> findActiveByEmail(@Param("email") String email);

    /**
     * Finds an active member by E.164 phone number.
     * Used during login and registration duplicate checks.
     */
    @Query("SELECT m FROM Member m WHERE m.phoneNumber = :phone AND m.deletedAt IS NULL")
    Optional<Member> findActiveByPhoneNumber(@Param("phone") String phone);

    /**
     * Convenience method that searches both email and phone in a single query.
     * Used by the authentication service for the login {@code identifier} field.
     */
    @Query("""
           SELECT m FROM Member m
           WHERE (m.email = :identifier OR m.phoneNumber = :identifier)
             AND m.deletedAt IS NULL
           """)
    Optional<Member> findActiveByIdentifier(@Param("identifier") String identifier);

    /**
     * Returns true if an active member already holds the given email.
     * Used for fast duplicate-check during registration without loading the entity.
     */
    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.email = :email AND m.deletedAt IS NULL")
    boolean existsActiveByEmail(@Param("email") String email);

    /**
     * Returns true if an active member already holds the given phone number.
     */
    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.phoneNumber = :phone AND m.deletedAt IS NULL")
    boolean existsActiveByPhoneNumber(@Param("phone") String phone);
}
