import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userManager } from '../auth/oidc';

/** Lands the Keycloak redirect, exchanges the code for tokens, and moves on. */
export function AuthCallback() {
    const navigate = useNavigate();
    const [error, setError] = useState('');

    useEffect(() => {
        userManager
            .signinRedirectCallback()
            .then((user) => {
                const target = (user.state as { from?: string } | undefined)?.from;
                navigate(target ?? '/discover', { replace: true });
            })
            .catch((e) => setError(e instanceof Error ? e.message : 'Sign-in failed.'));
    }, [navigate]);

    if (error) {
        return (
            <section className="center-page">
                <div className="card auth">
                    <h1>Sign-in failed.</h1>
                    <p className="muted">{error}</p>
                    <button onClick={() => navigate('/login', { replace: true })}>Try again</button>
                </div>
            </section>
        );
    }
    return (
        <section className="center-page">
            <p className="muted">Completing sign-in...</p>
        </section>
    );
}
