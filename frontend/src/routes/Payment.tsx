import { useEffect, useState } from 'react';
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import { api, SavedCard } from '../api';
import { useAuth } from '../auth/AuthContext';
import { Feedback } from '../components/Feedback';
import { statusMessage } from '../stations';
import { stripeConfigured, stripePromise } from '../billing/stripe';

export function Payment() {
    const [card, setCard] = useState<SavedCard | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = () => {
        setLoading(true);
        api.savedCard()
            .then((saved) => {
                setCard(saved);
                setError('');
            })
            .catch((e) => setError(statusMessage(e)))
            .finally(() => setLoading(false));
    };

    useEffect(load, []);

    return (
        <section className="narrow">
            <p className="eyebrow">PAYMENT METHOD</p>
            <h1>Keep your charge moving.</h1>
            <Feedback loading={loading} error={error} retry={load} />

            {!loading && card && (
                <div className="card payment">
                    <div className="fake-card">
                        <b>{card.brand.toUpperCase()}</b>
                        <span>•••• {card.last4}</span>
                    </div>
                    <p className="muted">
                        This card is charged automatically when a charging session ends.
                    </p>
                </div>
            )}

            {!loading && !card && !stripeConfigured && (
                <div className="card info">
                    <p className="muted">
                        Card capture is disabled: set VITE_STRIPE_PUBLISHABLE_KEY to enable it.
                    </p>
                </div>
            )}

            {!loading && !card && stripeConfigured && (
                <Elements
                    stripe={stripePromise}
                    options={{
                        // Deferred setup: the SetupIntent is created on the server
                        // only once the driver submits, so opening this page does
                        // not leave abandoned intents behind.
                        mode: 'setup',
                        currency: 'usd',
                        paymentMethodCreation: 'manual',
                        appearance: stripeAppearance,
                    }}
                >
                    <CardForm onSaved={setCard} />
                </Elements>
            )}
        </section>
    );
}

function CardForm({ onSaved }: { onSaved: (card: SavedCard) => void }) {
    const stripe = useStripe();
    const elements = useElements();
    const { user } = useAuth();
    const [consented, setConsented] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');

    const submit = async (event: React.FormEvent) => {
        event.preventDefault();
        if (!stripe || !elements) {
            return;
        }
        setSubmitting(true);
        setError('');
        try {
            const validation = await elements.submit();
            if (validation.error) {
                throw new Error(validation.error.message ?? 'Please check your card details.');
            }

            const email = (user?.profile.email as string) ?? 'driver@chargegrid.test';
            const name = (user?.profile.name as string) ?? 'ChargeGrid Driver';
            const intent = await api.setupIntent(email, name);

            const { error: confirmError } = await stripe.confirmSetup({
                elements,
                clientSecret: intent.clientSecret,
                confirmParams: { return_url: `${window.location.origin}/payment-method` },
                redirect: 'if_required',
            });
            if (confirmError) {
                throw new Error(confirmError.message ?? 'Could not save that card.');
            }

            onSaved(await api.saveCard(intent.setupIntentId));
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Could not save that card.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <form className="card payment" onSubmit={submit}>
            <PaymentElement />
            <label>
                <input
                    type="checkbox"
                    checked={consented}
                    onChange={(e) => setConsented(e.target.checked)}
                />{' '}
                I consent to usage-based charging after a session.
            </label>
            {error && <p className="muted">{error}</p>}
            <button
                className="primary"
                type="submit"
                disabled={!stripe || !consented || submitting}
            >
                {submitting ? 'Saving...' : 'Save payment method'}
            </button>
            <p className="muted">
                Card details are collected by Stripe Elements and never reach ChargeGrid. Test mode:
                use 4242 4242 4242 4242 with any future expiry and CVC.
            </p>
        </form>
    );
}

/** Matches Elements to the surrounding dark UI. */
const stripeAppearance = {
    theme: 'night' as const,
    variables: {
        colorPrimary: '#a4e96c',
        colorBackground: '#12161b',
        colorText: '#e8eef2',
        borderRadius: '6px',
    },
};
