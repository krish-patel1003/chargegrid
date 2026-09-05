import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, Session } from '../api';
import { Feedback } from '../components/Feedback';
import { statusMessage } from '../stations';

const CODE_LENGTH = 6;

export function SessionPage() {
    const { id = '' } = useParams();
    const navigate = useNavigate();
    const [session, setSession] = useState<Session | null>(null);
    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        api.session(id)
            .then(setSession)
            .catch((e) => setError(statusMessage(e)))
            .finally(() => setLoading(false));
    }, [id]);

    const stop = () => {
        setError('');
        api.verifyStop(id, code)
            .then(() => navigate('/activity'))
            .catch((e) => setError(statusMessage(e)));
    };

    return (
        <section className="narrow">
            <Feedback loading={loading} error={error} />
            {session && (
                <>
                    <div className="live">
                        <span>● LIVE SESSION</span>
                        <strong>{session.status}</strong>
                    </div>
                    <h1>Charging session</h1>
                    <div className="metrics">
                        <div>
                            <small>ENERGY</small>
                            <b>
                                {session.meterKwh.toFixed(2)} <i>kWh</i>
                            </b>
                        </div>
                        <div>
                            <small>SESSION</small>
                            <b>{session.id.slice(0, 8)}</b>
                        </div>
                        <div>
                            <small>EST. COST</small>
                            <b>${Number(session.cost).toFixed(2)}</b>
                        </div>
                    </div>
                    <label className="code-label">
                        Enter stop code
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
                        onClick={stop}
                    >
                        Stop charging
                    </button>
                </>
            )}
        </section>
    );
}
