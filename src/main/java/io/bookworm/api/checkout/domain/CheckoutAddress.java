package io.bookworm.api.checkout.domain;

import io.bookworm.api.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Delivery address captured during the checkout ADDRESS_SET step.
 * <p>
 * Why: the delivery address for an in-progress checkout session is a separate entity
 * (rather than embedded in CheckoutSession) so that it can be independently updated
 * without conflicting with the session's optimistic lock. It is distinct from
 * ordering.order_delivery_addresses — when the order is confirmed, the service
 * copies the data from here into an immutable OrderDeliveryAddress snapshot.
 * <p>
 * Side effects: only one active checkout address per session is permitted
 * (partial unique index). Re-setting the address soft-deletes the previous row
 * and inserts a new one.
 */
@Entity
@Table(
    schema = "checkout",
    name = "checkout_addresses",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_checkout_addresses_session",
                columnNames = "session_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CheckoutAddress extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checkout_address_id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID checkoutAddressId;

    /** The checkout session this address belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_checkout_addresses_session"))
    private CheckoutSession session;

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
