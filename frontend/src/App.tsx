import { Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { Activity } from './routes/Activity';
import { Discover } from './routes/Discover';
import { Login } from './routes/Login';
import { Payment } from './routes/Payment';
import { ReservationPage } from './routes/ReservationPage';
import { SessionPage } from './routes/SessionPage';
import { Simulator } from './routes/Simulator';
import { StationDetails } from './routes/StationDetails';

export function App() {
    return (
        <Layout>
            <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/discover" element={<Discover />} />
                <Route path="/stations/:id" element={<StationDetails />} />
                <Route path="/reservations/:id" element={<ReservationPage />} />
                <Route path="/sessions/:id" element={<SessionPage />} />
                <Route path="/activity" element={<Activity />} />
                <Route path="/payment-method" element={<Payment />} />
                <Route path="/admin/stations/:id/simulator" element={<Simulator />} />
                <Route path="*" element={<Discover />} />
            </Routes>
        </Layout>
    );
}
