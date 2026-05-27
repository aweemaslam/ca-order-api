package com.caorderapi.feign.dto;

/**
 * Payment charge response modelled after Stripe PaymentIntents / Adyen Payments API.
 *
 * <ul>
 *   <li>{@code transactionId}     – PSP-assigned unique transaction reference (Adyen: pspReference, Stripe: PaymentIntent id)</li>
 *   <li>{@code merchantReference} – echoed back order reference for correlation</li>
 *   <li>{@code resultCode}        – outcome code: AUTHORISED, REFUSED, ERROR, CANCELLED (Adyen resultCode / Stripe status)</li>
 *   <li>{@code refusalReason}     – human-readable decline reason when resultCode is REFUSED (null on success)</li>
 *   <li>{@code authCode}          – issuer authorisation code provided on successful authorisation</li>
 * </ul>
 */
public record PaymentChargeResponse(
        String transactionId,
        String merchantReference,
        String resultCode,
        String refusalReason,
        String authCode
) {
}
