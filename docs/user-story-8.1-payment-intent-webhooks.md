# User Story 8.1 — PaymentIntent + verified webhooks

**Status: implemented** on branch `feature/us-8.1-payment-intent-webhooks`.

## Goal

Process paid bookings without storing card details: create a Stripe PaymentIntent, let the client confirm payment, and only mark seats `BOOKED` after a **verified** webhook.

## Flow

1. Customer holds seats → checkout `POST /api/events/{id}/bookings`
2. Backend creates booking `PENDING_PAYMENT` + PaymentIntent; returns `clientSecret`
3. Frontend confirms with Stripe.js (`PaymentElement`)
4. Stripe → `POST /api/payments/stripe/webhook` (signature verified)
5. Backend confirms booking, seats `HELD` → `BOOKED`
6. Frontend polls `GET /api/bookings/{id}` until `CONFIRMED`

## Providers

| `payment.provider` | Behaviour |
|---|---|
| `mock` (default) | Fake PI id; confirms in-process (demo / CI) |
| `stripe` | Real PaymentIntent + webhook confirmation |

Copy [`application-local.properties.example`](../src/main/resources/application-local.properties.example) to `application-local.properties` (gitignored) and run:

```bash
stripe listen --forward-to localhost:8080/api/payments/stripe/webhook
```

Use test card `4242 4242 4242 4242`.
