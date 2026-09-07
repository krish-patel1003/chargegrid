import { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function Layout({ children }: { children: ReactNode }) {
    const { user, logout } = useAuth();
    const name = user?.profile.preferred_username ?? user?.profile.email;

    return (
        <>
            <header>
                <Link to={user ? '/discover' : '/login'} className="brand">
                    CHARGE<span>GRID</span>
                </Link>
                <nav>
                    {user ? (
                        <>
                            <Link to="/activity">Activity</Link>
                            <Link to="/payment-method">Payment</Link>
                            <span className="muted">{name}</span>
                            <button className="nav-cta" onClick={() => logout()}>
                                Log out
                            </button>
                        </>
                    ) : (
                        <Link to="/login" className="nav-cta">
                            Log in
                        </Link>
                    )}
                </nav>
            </header>
            <main>{children}</main>
        </>
    );
}
