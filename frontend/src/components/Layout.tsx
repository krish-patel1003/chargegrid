import { ReactNode } from 'react';
import { Link } from 'react-router-dom';

export function Layout({ children }: { children: ReactNode }) {
    return (
        <>
            <header>
                <Link to="/discover" className="brand">
                    CHARGE<span>GRID</span>
                </Link>
                <nav>
                    <Link to="/activity">Activity</Link>
                    <Link to="/payment-method">Payment</Link>
                    <Link to="/login" className="nav-cta">
                        Log in
                    </Link>
                </nav>
            </header>
            <main>{children}</main>
        </>
    );
}
