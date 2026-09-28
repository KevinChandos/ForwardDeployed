package io.bookworm.api.payment.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * An individual debit or credit entry in a WalletAccount's ledger.
 * <p>
 * Why: wallet transactions form an immutable ledger — they are inserted once and
 * never modified. The {@code balanceAfter} snapshot provides a running balance for
 * reconciliation without needing to sum all transactions. The {@code referenceId}
 * links the transaction to its source (e.g. a refund_id or order_id).
 * <p>
 * Side effects: this table should be treated as append-only. Any wallet balance
 * change must produce a WalletTransaction row in the same DB transaction as the
 * WalletAccount balance update to maintain ledger consistency.
 */
@Entity
@Table(schema = "wallet", name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WalletTransaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_txn_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID walletTxnId;

    /** The wallet account this transaction belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wallet_txn_wallet"))
    private WalletAccount wallet;

    /**
     * Direction of the transaction. DB CHECK: CREDIT or DEBIT.
     * The amount is always stored as a positive value; direction is from this field.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 10)
    private TxnType txnType;

    /**
     * Transaction amount. Always positive; direction determined by {@code txnType}.
     * DB CHECK: amount > 0.
     */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /**
     * Business source of this transaction.
     * DB CHECK enforces allowed values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private TxnSource source;

    /**
     * Optional UUID of the originating entity (e.g. refund_id for REFUND source,
     * order_id for REDEMPTION source).
     */
    @Column(name = "reference_id")
    private UUID referenceId;

    /**
     * Running balance of the wallet after this transaction was applied.
     * Used for ledger reconciliation without summing the entire history.
     */
    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    // ── Enums ───────────────────────────────────────────────────────────────

    /** Transaction direction. */
    public enum TxnType {
        CREDIT,
        DEBIT
    }

    /** Business event that triggered the transaction. */
    public enum TxnSource {
        REFUND,
        GIFT,
        REDEMPTION,
        ADJUSTMENT
    }
}
