package io.bookworm.api.store.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Time-bounded tax rule for a specific tax category within a Store.
 * <p>
 * Why: tax rates vary by jurisdiction and product category (e.g. books have
 * 5% GST in India while default goods may have 18%). Storing them as time-bounded
 * rows allows tax rate changes without modifying historical order calculations.
 * <p>
 * Side effects: the current applicable rate is the row where
 * {@code effectiveTo IS NULL} for a given {@code (store_id, tax_category)} pair.
 * {@code ratePercent} is stored as NUMERIC(5,2) — e.g. 5.00 represents 5%.
 */
@Entity
@Table(schema = "store", name = "tax_rules")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TaxRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tax_rule_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID taxRuleId;

    /** The store this tax rule applies to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tax_rules_store"))
    private Store store;

    /**
     * Product tax category (e.g. "BOOKS", "DEFAULT").
     * Maps to the applicable tax bracket for the jurisdiction.
     */
    @Column(name = "tax_category", nullable = false, length = 100)
    private String taxCategory;

    /**
     * Applicable tax rate as a percentage. Range: 0.00–100.00.
     * DB CHECK: rate_percent >= 0 AND rate_percent <= 100.
     */
    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercent;

    /** Date from which this rule is effective (inclusive). */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Date until which this rule is effective (exclusive). NULL = currently active.
     */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
