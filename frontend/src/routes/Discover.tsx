import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { Feedback } from '../components/Feedback';
import { MapView } from '../components/MapView';
import { cacheStations, demoStations, normalize } from '../stations';

export function Discover() {
    const [availableOnly, setAvailableOnly] = useState(false);
    const [stations, setStations] = useState(demoStations);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = () => {
        setLoading(true);
        api.nearby()
            .then((data) => {
                const next = data.map(normalize);
                cacheStations(next);
                setStations(next);
                setError('');
            })
            .catch(() => {
                setStations(demoStations);
                setError('API unavailable. Showing demo stations.');
            })
            .finally(() => setLoading(false));
    };

    useEffect(load, []);

    const useMyLocation = () =>
        navigator.geolocation?.getCurrentPosition((position) =>
            api
                .nearby(position.coords.latitude, position.coords.longitude)
                .then((data) => {
                    const next = data.map(normalize);
                    cacheStations(next);
                    setStations(next);
                })
                .catch(() => undefined),
        );

    const visible = stations.filter(
        (station) => !availableOnly || station.connectors.some((c) => c.available),
    );

    return (
        <section>
            <div className="page-heading">
                <div>
                    <p className="eyebrow">DISCOVER</p>
                    <h1>Find your next charge.</h1>
                </div>
                <button className="outline" onClick={useMyLocation}>
                    ◎ Use my location
                </button>
            </div>
            <div className="searchbar">
                <input placeholder="Search a city or address" />
                <select>
                    <option>All connectors</option>
                    <option>CCS</option>
                    <option>J1772</option>
                </select>
                <label>
                    <input
                        type="checkbox"
                        checked={availableOnly}
                        onChange={(e) => setAvailableOnly(e.target.checked)}
                    />{' '}
                    Available now
                </label>
            </div>
            <Feedback loading={loading} error={error} retry={load} />
            <div className="discover-grid">
                <MapView stations={visible} />
                <div className="station-list">
                    <p className="muted">{visible.length} stations nearby</p>
                    {visible.map((station) => (
                        <Link
                            className="station-card"
                            to={`/stations/${station.id}`}
                            key={station.id}
                        >
                            <div>
                                <h3>{station.name}</h3>
                                <p>{station.address}</p>
                            </div>
                            <strong>
                                {station.connectors.filter((c) => c.available).length}
                                <small> available</small>
                            </strong>
                            <span>›</span>
                        </Link>
                    ))}
                </div>
            </div>
        </section>
    );
}
