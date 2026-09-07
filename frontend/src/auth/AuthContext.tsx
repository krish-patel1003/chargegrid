import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from 'react';
import { User } from 'oidc-client-ts';
import { userManager } from './oidc';

type AuthState = {
    user: User | null;
    loading: boolean;
    login: () => Promise<void>;
    logout: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let active = true;

        userManager
            .getUser()
            .then((existing) => {
                if (active) {
                    setUser(existing && !existing.expired ? existing : null);
                }
            })
            .catch(() => active && setUser(null))
            .finally(() => {
                if (active) {
                    setLoading(false);
                }
            });

        const onLoaded = (next: User) => setUser(next);
        const onCleared = () => setUser(null);
        userManager.events.addUserLoaded(onLoaded);
        userManager.events.addUserUnloaded(onCleared);
        userManager.events.addAccessTokenExpired(onCleared);

        return () => {
            active = false;
            userManager.events.removeUserLoaded(onLoaded);
            userManager.events.removeUserUnloaded(onCleared);
            userManager.events.removeAccessTokenExpired(onCleared);
        };
    }, []);

    const value = useMemo<AuthState>(
        () => ({
            user,
            loading,
            login: () => userManager.signinRedirect(),
            logout: () => userManager.signoutRedirect(),
        }),
        [user, loading],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used inside an AuthProvider');
    }
    return context;
}
