import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, Session } from '../api';
import { Feedback } from '../components/Feedback';
import { cacheStation, normalize, resolveStation, statusMessage } from '../stations';

export function Activity() {
    const [sessions, setSessions] = useState<Session[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = () => {
        setLoading(true);
        api.sessions()
            .then(async (data) => {
                setSessions(data);
                setError('');
                // History can be opened directly, with nothing cached from the
                // discovery screen, so pull in the stations these sessions name.
                const missing = [...new Set(data.map((s) => s.stationId))].filter(
                    (id) => id && !resolveStation(id, undefined),
                );
                const fetched = await Promise.all(
                    missing.map((id) => api.station(id).catch(() => null)),
                );
                fetched.forEach((station) => station && cacheStation(normalize(station)));
                if (fetched.some(Boolean)) {
                    setSessions([...data]);
                }
            })
            .catch((e) => setError(statusMessage(e)))
            .finally(() => setLoading(false));
    };

    useEffect(load, []);

    return (
        <section className="narrow">
            <p className="eyebrow">ACTIVITY</p>
            <h1>Your charging history.</h1>
            <Feedback loading={loading} error={error} retry={load} />
            {!loading && !error && sessions.length === 0 && (
                <div className="card info">
                    <p className="muted">No charging sessions yet.</p>
                    <Link to="/discover" className="outline button-link">
                        Find a station
                    </Link>
                </div>
            )}
            {sessions.map((session) => (
                <div className="card history" key={session.id}>
                    <div>
                        <b>
                            {resolveStation(session.stationId, session.connectorId)?.name ??
                                'Charging session'}
                        </b>
                        <p>
                            {new Date(session.startedAt).toLocaleString()} ·{' '}
                            {session.meterKwh.toFixed(2)} kWh · {session.status}
                        </p>
                    </div>
                    <strong>${Number(session.cost).toFixed(2)}</strong>
                </div>
            ))}
        </section>
    );
}
