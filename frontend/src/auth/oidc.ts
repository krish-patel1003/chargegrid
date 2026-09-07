import { User, UserManager, WebStorageStateStore } from 'oidc-client-ts';

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080';
const REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'chargegrid';
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'chargegrid-frontend';

export const CALLBACK_PATH = '/auth/callback';

/**
 * Authorization Code flow with PKCE. The client is public, so there is no secret
 * in the bundle: the code verifier is what proves this browser started the login.
 */
export const userManager = new UserManager({
    authority: `${KEYCLOAK_URL}/realms/${REALM}`,
    client_id: CLIENT_ID,
    redirect_uri: `${window.location.origin}${CALLBACK_PATH}`,
    post_logout_redirect_uri: `${window.location.origin}/login`,
    response_type: 'code',
    scope: 'openid profile email',
    // Tokens live in sessionStorage rather than localStorage so they do not
    // outlive the tab, and are renewed in the background before they expire.
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    automaticSilentRenew: true,
});

/** Access token for the current session, or null when signed out. */
export async function accessToken(): Promise<string | null> {
    const user = await userManager.getUser();
    return user && !user.expired ? user.access_token : null;
}

export type { User };
