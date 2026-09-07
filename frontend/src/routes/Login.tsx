import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { userManager } from '../auth/oidc';

export function Login() {
    const { user, loading } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const from = (location.state as { from?: string } | null)?.from;

    useEffect(() => {
        if (!loading && user) {
            navigate(from ?? '/discover', { replace: true });
        }
    }, [user, loading, from, navigate]);

    return (
        <section className="center-page">
            <div className="card auth">
                <p className="eyebrow">YOUR CHARGING NETWORK</p>
                <h1>
                    Power up, <i>without</i> the wait.
                </h1>
                <p>Find a connector, reserve it for ten minutes, and get moving.</p>
                <button onClick={() => userManager.signinRedirect({ state: { from } })}>
                    Continue with Keycloak
                </button>
                <small>Demo realm: sign in as `driver` / `driver`.</small>
            </div>
        </section>
    );
}
