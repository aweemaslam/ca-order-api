package com.caorderapi.feign.dto;

/**
 * Payment charge request modelled after Stripe PaymentIntents / Adyen Payments API.
 *
 * @param merchantReference unique order identifier used as idempotency key
 * @param paymentIntentId PSP payment intent identifier used for auth/capture lifecycle tracking
 * @param amountCents amount in the smallest currency unit (e.g. 4999 = €49.99) as required by Stripe and Adyen
 * @param currency ISO 4217 three-letter currency code (e.g. EUR, USD, GBP)
 * @param shopperEmail customer email used for shopper identification and receipt (Adyen: shopperEmail, Stripe: receipt_email)
 * @param description human-readable charge description shown on bank statements
 * @param captureMode IMMEDIATE (auto-capture) or MANUAL (authorise only, capture later)
 */
public record PaymentChargeRequest(
        String merchantReference,
        String paymentIntentId,
        long amountCents,
        String currency,
        String shopperEmail,
        String description,
        CaptureMode captureMode
) {
    public enum CaptureMode {
        IMMEDIATE,
        MANUAL
    }
}
