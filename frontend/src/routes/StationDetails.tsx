import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api';
import { Feedback } from '../components/Feedback';
import { cacheStation, findStation, normalize, statusMessage } from '../stations';
import { Station } from '../types';

export function StationDetails() {
    const { id = '' } = useParams();
    const navigate = useNavigate();
    const [station, setStation] = useState<Station | undefined>(() => findStation(id));
    const [loading, setLoading] = useState(true);
    const [reserving, setReserving] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        api.station(id)
            .then((data) => {
                const next = normalize(data);
                cacheStation(next);
                setStation(next);
            })
            .catch((e) => {
                if (!findStation(id)) {
                    setError(statusMessage(e));
                }
            })
            .finally(() => setLoading(false));
    }, [id]);

    if (!station) {
        return (
            <section className="narrow">
                <Feedback loading={loading} error={error || 'Station not found.'} />
                <Link to="/discover" className="back">
                    ← Back to discovery
                </Link>
            </section>
        );
    }

    const reserve = (connectorId: string) => {
        setReserving(true);
        setError('');
        api.reserve(connectorId)
            .then((reservation) => navigate(`/reservations/${reservation.id}`))
            .catch((e) => setError(statusMessage(e)))
            .finally(() => setReserving(false));
    };

    return (
        <section className="narrow">
            <Link to="/discover" className="back">
                ← Back to discovery
            </Link>
            <p className="eyebrow">STATION DETAILS</p>
            <h1>{station.name}</h1>
            <p className="lead">{station.address}</p>
            <div className="actions">
                <button
                    className="outline"
                    onClick={() => navigator.clipboard?.writeText(station.address)}
                >
                    Copy address
                </button>
                <a
                    className="outline button-link"
                    href={`https://www.google.com/maps/dir/?api=1&destination=${station.lat},${station.lng}`}
                    target="_blank"
                    rel="noreferrer"
                >
                    Open in Google Maps ↗
                </a>
            </div>
            {loading && <p className="muted">Refreshing station details...</p>}
            {error && <p className="muted">{error}</p>}
            <h2>Choose a connector</h2>
            {station.connectors.map((connector) => (
                <div className="connector" key={connector.id}>
                    <div>
                        <b>{connector.type}</b>
                        <p>
                            {connector.power} kW maximum · ${connector.price.toFixed(2)} / kWh
                        </p>
                    </div>
                    <span className={connector.available ? 'available' : 'unavailable'}>
                        {connector.available ? 'Available' : 'In use'}
                    </span>
                    {connector.available && (
                        <button disabled={reserving} onClick={() => reserve(connector.id)}>
                            {reserving ? 'Reserving...' : 'Reserve'}
                        </button>
                    )}
                </div>
            ))}
        </section>
    );
}
