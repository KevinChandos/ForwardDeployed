package io.bookworm.api.catalogue.infrastructure;

import io.bookworm.api.catalogue.domain.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Publisher} entities.
 */
@Repository
public interface PublisherRepository extends JpaRepository<Publisher, UUID> {

    /** Finds an active publisher by ID. */
    @Query("SELECT p FROM Publisher p WHERE p.publisherId = :id AND p.deletedAt IS NULL")
    Optional<Publisher> findActive(@Param("id") UUID id);

    /** Pages through all active publishers. */
    @Query(value = "SELECT p FROM Publisher p WHERE p.deletedAt IS NULL",
           countQuery = "SELECT COUNT(p) FROM Publisher p WHERE p.deletedAt IS NULL")
    Page<Publisher> findAllActive(Pageable pageable);

    /** Checks whether a publisher name is already taken among active records. */
    @Query("SELECT COUNT(p) > 0 FROM Publisher p WHERE p.name = :name AND p.deletedAt IS NULL")
    boolean existsActiveByName(@Param("name") String name);
}
