package io.bookworm.api.common.exception;

/**
 * Thrown when a payment gateway rejects a charge attempt.
 * <p>
 * Why: Subclasses {@link BusinessRuleException} so the global handler returns HTTP 422
 * with a {@code PAYMENT_DECLINED} code.  Carries the gateway's decline reason so that the
 * handler can include actionable detail (e.g. "Insufficient funds") without leaking raw
 * gateway error objects upstream.
 * <p>
 * Side effects: Triggers transaction rollback; the order state must be handled by the
 * caller (e.g. transition to PAYMENT_FAILED) before throwing.
 */
public class PaymentDeclinedException extends BusinessRuleException {

    private final String gatewayCode;

    public PaymentDeclinedException(String gatewayCode, String reason) {
        super("PAYMENT_DECLINED", reason);
        this.gatewayCode = gatewayCode;
    }

    /** The raw decline code returned by the payment processor (e.g. "insufficient_funds"). */
    public String getGatewayCode() {
        return gatewayCode;
    }
}
