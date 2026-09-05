import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, Reservation } from '../api';
import { Feedback } from '../components/Feedback';
import { findByConnector, statusMessage } from '../stations';

const CODE_LENGTH = 6;

function useCountdown(expiresAt: string | undefined) {
    const [now, setNow] = useState(() => Date.now());

    useEffect(() => {
        const timer = window.setInterval(() => setNow(Date.now()), 1000);
        return () => window.clearInterval(timer);
    }, []);

    return useMemo(() => {
        if (!expiresAt) {
            return null;
        }
        const remaining = Math.max(0, new Date(expiresAt).getTime() - now);
        const minutes = Math.floor(remaining / 60_000);
        const seconds = Math.floor((remaining % 60_000) / 1000);
        return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }, [expiresAt, now]);
}

export function ReservationPage() {
    const { id = '' } = useParams();
    const navigate = useNavigate();
    const [reservation, setReservation] = useState<Reservation | null>(null);
    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const remaining = useCountdown(reservation?.expiresAt);

    useEffect(() => {
        api.reservation(id)
            .then(setReservation)
            .catch((e) => setError(statusMessage(e)))
            .finally(() => setLoading(false));
    }, [id]);

    const start = () => {
        setError('');
        api.verifyStart(id, code)
            .then((session) => navigate(`/sessions/${session.id}`))
            .catch((e) => setError(statusMessage(e)));
    };

    const station = findByConnector(reservation?.connectorId);
    const connector =
        station.connectors.find((c) => c.id === reservation?.connectorId) ?? station.connectors[0];

    return (
        <section className="narrow">
            <Feedback loading={loading} error={error} />
            {reservation && (
                <>
                    <p className="eyebrow">RESERVATION {reservation.status}</p>
                    <h1>Your connector is waiting.</h1>
                    <div className="timer">
                        {remaining ?? '--:--'}
                        <small> remaining</small>
                    </div>
                    <div className="card info">
                        <b>
                            {connector.type} · {connector.power} kW
                        </b>
                        <p>
                            {station.name}
                            <br />
                            {station.address}
                        </p>
                    </div>
                    <label className="code-label">
                        Enter start code
                        <input
                            value={code}
                            onChange={(e) => setCode(e.target.value)}
                            placeholder="6-digit code"
                            maxLength={CODE_LENGTH}
                        />
                    </label>
                    {error && <p className="muted">{error}</p>}
                    <button
                        className="button-link primary"
                        disabled={code.length !== CODE_LENGTH}
                        onClick={start}
                    >
                        Start charging
                    </button>
                </>
            )}
        </section>
    );
}
