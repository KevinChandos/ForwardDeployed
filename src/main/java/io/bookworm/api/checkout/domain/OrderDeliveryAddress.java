package io.bookworm.api.checkout.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Immutable snapshot of the delivery address for a confirmed Order.
 * <p>
 * Why: order delivery addresses must never change after an order is confirmed —
 * shipping providers have the address at dispatch time. Using a snapshot entity
 * (distinct from the member's mutable MemberAddress) guarantees that changes to
 * the member's address book do not retroactively alter historical order records.
 * <p>
 * Side effects: one-to-one with Order; the unique partial index on order_id
 * enforces one active delivery address per order. Soft-delete is included per
 * the standard schema but should never be used on confirmed orders.
 */
@Entity
@Table(
    schema = "ordering",
    name = "order_delivery_addresses",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_order_delivery_addresses_order",
                columnNames = "order_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderDeliveryAddress extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "delivery_address_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID deliveryAddressId;

    /** The order this delivery address belongs to. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_delivery_addresses_order"))
    private Order order;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "line1", nullable = false, length = 250)
    private String line1;

    @Column(name = "line2", length = 250)
    private String line2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "pin_code", nullable = false, length = 20)
    private String pinCode;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "country", nullable = false, columnDefinition = "CHAR(2)")
    private String country = "IN";

    /** Contact email for delivery notifications. */
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    /** Contact phone number for the courier. */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
}
