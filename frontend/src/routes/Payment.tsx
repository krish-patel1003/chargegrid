export function Payment() {
    return (
        <section className="narrow">
            <p className="eyebrow">PAYMENT METHOD</p>
            <h1>Keep your charge moving.</h1>
            <div className="card payment">
                <div className="fake-card">
                    <b>VISA</b>
                    <span>•••• 4242</span>
                </div>
                <label>
                    <input type="checkbox" defaultChecked /> I consent to usage-based charging after
                    a session.
                </label>
                <button className="primary">Save payment method</button>
                <p className="muted">
                    Demo screen: card capture is not wired to Stripe Elements yet. ChargeGrid never
                    stores card numbers or CVC.
                </p>
            </div>
        </section>
    );
}
