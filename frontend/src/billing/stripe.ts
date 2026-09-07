import { loadStripe, Stripe } from '@stripe/stripe-js';

const PUBLISHABLE_KEY = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;

/**
 * Loaded once and shared. The publishable key is designed to be public: it can
 * only create payment methods, never move money, so it belongs in the bundle.
 * Card details go straight from Elements to Stripe and never touch our servers.
 */
export const stripePromise: Promise<Stripe | null> = PUBLISHABLE_KEY
    ? loadStripe(PUBLISHABLE_KEY)
    : Promise.resolve(null);

export const stripeConfigured = Boolean(PUBLISHABLE_KEY);
