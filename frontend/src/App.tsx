import { Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { RequireAuth } from './auth/RequireAuth';
import { CALLBACK_PATH } from './auth/oidc';
import { Layout } from './components/Layout';
import { Activity } from './routes/Activity';
import { AuthCallback } from './routes/AuthCallback';
import { Discover } from './routes/Discover';
import { Login } from './routes/Login';
import { Payment } from './routes/Payment';
import { ReservationPage } from './routes/ReservationPage';
import { SessionPage } from './routes/SessionPage';
import { Simulator } from './routes/Simulator';
import { StationDetails } from './routes/StationDetails';

/** Everything except sign-in and the OIDC callback needs a session. */
const protectedRoutes: [string, React.ReactNode][] = [
    ['/discover', <Discover />],
    ['/stations/:id', <StationDetails />],
    ['/reservations/:id', <ReservationPage />],
    ['/sessions/:id', <SessionPage />],
    ['/activity', <Activity />],
    ['/payment-method', <Payment />],
    ['/admin/stations/:id/simulator', <Simulator />],
    ['*', <Discover />],
];

export function App() {
    return (
        <AuthProvider>
            <Layout>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path={CALLBACK_PATH} element={<AuthCallback />} />
                    {protectedRoutes.map(([path, element]) => (
                        <Route
                            key={path}
                            path={path}
                            element={<RequireAuth>{element}</RequireAuth>}
                        />
                    ))}
                </Routes>
            </Layout>
        </AuthProvider>
    );
}
