package io.bookworm.api.payment.domain;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A member's wallet account — stores the available balance for use at checkout.
 * <p>
 * Why: the wallet is a first-class entity rather than a column on Member so that
 * balance changes carry a full audit trail (via WalletTransaction) and so the
 * wallet can be individually frozen without affecting the member's account status.
 * One wallet per member is enforced by a partial unique index.
 * <p>
 * Side effects: {@code balance} must always be >= 0 (DB CHECK). FROZEN wallets
 * cannot be debited or credited; the service layer must check status before any
 * wallet operation. {@code balance} must only be mutated via a WalletTransaction
 * insert in the same DB transaction to maintain auditability.
 */
@Entity
@Table(
    schema = "wallet",
    name = "wallet_accounts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_wallet_accounts_member", columnNames = "member_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WalletAccount extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID walletId;

    /** The member who owns this wallet. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wallet_accounts_member"))
    private Member member;

    /**
     * Current available balance. Must be >= 0 (DB CHECK).
     * Only update via WalletTransaction in the same transaction.
     */
    @Column(name = "balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Currency of the wallet balance (ISO-4217). */
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "INR";

    /** Wallet status. FROZEN prevents all credits and debits. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;

    /** Transaction history for this wallet. */
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WalletTransaction> transactions = new ArrayList<>();

    // ── Enum ────────────────────────────────────────────────────────────────

    /** Wallet account states. */
    public enum WalletStatus {
        ACTIVE,
        FROZEN
    }
}
