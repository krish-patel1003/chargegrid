import { useEffect, useRef, useState } from 'react';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { Station } from '../types';

const MAP_STYLE = import.meta.env.VITE_MAP_STYLE || 'https://demotiles.maplibre.org/style.json';
const MARKER_COLOR = '#a4e96c';

export function MapView({ stations }: { stations: Station[] }) {
    const container = useRef<HTMLDivElement>(null);
    const [tilesUnavailable, setTilesUnavailable] = useState(false);

    useEffect(() => {
        if (!container.current) {
            return;
        }
        const map = new maplibregl.Map({
            container: container.current,
            style: MAP_STYLE,
            center: [-118.1937, 33.7701],
            zoom: 12,
        });
        map.addControl(new maplibregl.NavigationControl(), 'top-right');
        // Basemap tiles come from a third party; the surrounding UI stays usable
        // when they cannot be reached.
        map.on('error', (event) => {
            if (event.error?.message?.includes('Failed to fetch')) {
                setTilesUnavailable(true);
            }
        });
        stations.forEach((station) =>
            new maplibregl.Marker({ color: MARKER_COLOR })
                .setLngLat([station.lng, station.lat])
                .setPopup(new maplibregl.Popup().setText(station.name))
                .addTo(map),
        );
        return () => map.remove();
    }, [stations]);

    return (
        <div className="map-wrap">
            <div className="map" ref={container} />
            {tilesUnavailable && (
                <p className="map-note muted">
                    Basemap tiles unavailable offline — station markers still load from the API.
                </p>
            )}
        </div>
    );
}
