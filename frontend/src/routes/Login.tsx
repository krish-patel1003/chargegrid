import { useNavigate } from 'react-router-dom';

export function Login() {
    const navigate = useNavigate();
    return (
        <section className="center-page">
            <div className="card auth">
                <p className="eyebrow">YOUR CHARGING NETWORK</p>
                <h1>
                    Power up, <i>without</i> the wait.
                </h1>
                <p>Find a connector, reserve it for ten minutes, and get moving.</p>
                <button onClick={() => navigate('/discover')}>Continue with Keycloak</button>
                <small>
                    Demo mode is enabled. Production login uses the configured OIDC client.
                </small>
            </div>
        </section>
    );
}
