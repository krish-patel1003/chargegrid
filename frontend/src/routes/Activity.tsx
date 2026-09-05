import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, Session } from '../api';
import { Feedback } from '../components/Feedback';
import { findByConnector, statusMessage } from '../stations';

export function Activity() {
    const [sessions, setSessions] = useState<Session[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = () => {
        setLoading(true);
        api.sessions()
            .then((data) => {
                setSessions(data);
                setError('');
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
                        <b>{findByConnector(session.connectorId).name}</b>
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
