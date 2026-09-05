import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, Simulator as SimulatorView } from '../api';
import { statusMessage } from '../stations';

/**
 * Operator display standing in for the physical charger: it shows the codes the
 * hardware would print and lets an operator push meter readings.
 */
export function Simulator() {
    const { id = '' } = useParams();
    const [view, setView] = useState<SimulatorView | null>(null);
    const [kwh, setKwh] = useState('1');
    const [error, setError] = useState('');

    const load = () =>
        api
            .simulator(id)
            .then((data) => {
                setView(data);
                setError('');
            })
            .catch((e) => setError(statusMessage(e)));

    useEffect(() => {
        load();
    }, [id]);

    const meter = (sessionId: string) =>
        api
            .meter(sessionId, Number(kwh))
            .then(load)
            .catch((e) => setError(statusMessage(e)));

    return (
        <section className="narrow simulator">
            <p className="eyebrow">STATION SIMULATOR · OPERATOR DISPLAY</p>
            <h1>Station simulator</h1>
            <p className="lead">Physical charger control panel</p>
            {error && (
                <div className="card info">
                    <p className="muted">{error}</p>
                    <button className="outline" onClick={load}>
                        Try again
                    </button>
                </div>
            )}
            {view && (
                <div className="card info">
                    <small>ACTIVE RESERVATIONS</small>
                    {view.reservations.map((reservation) => (
                        <p key={reservation.id}>
                            {reservation.connectorId} · {reservation.status}
                            <br />
                            <b className="sim-code">{reservation.startCode}</b>
                        </p>
                    ))}
                    <small>ACTIVE SESSIONS</small>
                    {view.sessions.map((session) => (
                        <div key={session.id}>
                            <p>
                                {session.id} · {session.meterKwh.toFixed(2)} kWh
                            </p>
                            <input
                                type="number"
                                min="0.01"
                                step="0.01"
                                value={kwh}
                                onChange={(e) => setKwh(e.target.value)}
                            />
                            <button className="outline" onClick={() => meter(session.id)}>
                                Add meter reading
                            </button>
                            <p>
                                <b className="sim-code stop">{session.stopCode}</b>
                            </p>
                        </div>
                    ))}
                </div>
            )}
        </section>
    );
}
