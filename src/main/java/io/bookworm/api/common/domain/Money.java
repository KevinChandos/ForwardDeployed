package io.bookworm.api.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

/**
 * Value object representing a monetary amount with an ISO-4217 currency code.
 * <p>
 * Why: Money is embedded rather than mapped to a separate table so that every
 * monetary column pair (amount + currency) lives in the owning row, avoiding
 * joins for simple reads. BigDecimal is required to prevent IEEE-754 precision
 * loss that occurs with double/float for financial arithmetic.
 * <p>
 * Side effects: any entity that embeds this type must use {@code @AttributeOverrides}
 * if more than one Money field is present, to avoid duplicate column name conflicts.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Money {

    /**
     * Decimal amount stored as NUMERIC(14,2) in PostgreSQL.
     * Must be strictly positive for prices; use separate validation at the entity
     * level for fields that permit zero (e.g. discount_amount, wallet_debit_amount).
     */
    @NotNull
    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount;

    /**
     * ISO-4217 three-letter currency code (e.g. "INR", "USD").
     * Stored as CHAR(3). Defaults to INR per design spec.
     */
    @NotBlank
    @Column(name = "currency", length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    /**
     * Factory for INR amounts — the default currency in this platform.
     *
     * @param amount decimal amount; must not be null
     * @return a Money instance with INR currency
     */
    public static Money inr(BigDecimal amount) {
        return new Money(amount, "INR");
    }

    /**
     * Factory for zero-value INR — useful for default discount/shipping fields.
     */
    public static Money zeroInr() {
        return new Money(BigDecimal.ZERO, "INR");
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
