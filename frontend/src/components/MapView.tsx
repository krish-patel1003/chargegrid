import { useEffect, useRef, useState } from 'react';
import maplibregl from 'maplibre-gl';
import { useNavigate } from 'react-router-dom';
import 'maplibre-gl/dist/maplibre-gl.css';
import { Station } from '../types';

// CARTO's dark basemap: real OpenStreetMap data, vector tiles, no API key, and
// a palette that matches the rest of the UI. Override for a different provider.
const MAP_STYLE =
    import.meta.env.VITE_MAP_STYLE ||
    'https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json';
const AVAILABLE = '#a4e96c';
const IN_USE = '#6b7280';
const FALLBACK_CENTER: [number, number] = [-118.1937, 33.7701];

export function MapView({ stations }: { stations: Station[] }) {
    const container = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();
    const [tilesUnavailable, setTilesUnavailable] = useState(false);

    useEffect(() => {
        if (!container.current) {
            return;
        }

        const map = new maplibregl.Map({
            container: container.current,
            style: MAP_STYLE,
            center: FALLBACK_CENTER,
            zoom: 12,
            // OpenStreetMap and CARTO both require visible credit.
            attributionControl: { compact: true },
        });
        map.addControl(new maplibregl.NavigationControl(), 'top-right');

        map.on('style.load', () => setTilesUnavailable(false));
        map.on('error', () => {
            if (!map.isStyleLoaded()) {
                setTilesUnavailable(true);
            }
        });

        const markers = stations.map((station) => {
            const free = station.connectors.filter((c) => c.available).length;
            const popup = new maplibregl.Popup({ offset: 24 }).setHTML(
                `<strong>${escapeHtml(station.name)}</strong><br/>${free} of ${
                    station.connectors.length
                } available`,
            );
            const marker = new maplibregl.Marker({ color: free > 0 ? AVAILABLE : IN_USE })
                .setLngLat([station.lng, station.lat])
                .setPopup(popup)
                .addTo(map);
            marker.getElement().style.cursor = 'pointer';
            marker
                .getElement()
                .addEventListener('dblclick', () => navigate(`/stations/${station.id}`));
            return marker;
        });

        // Frame whatever came back from the search rather than a hardcoded centre.
        if (stations.length === 1) {
            map.setCenter([stations[0].lng, stations[0].lat]);
            map.setZoom(14);
        } else if (stations.length > 1) {
            const bounds = stations.reduce(
                (acc, s) => acc.extend([s.lng, s.lat]),
                new maplibregl.LngLatBounds(
                    [stations[0].lng, stations[0].lat],
                    [stations[0].lng, stations[0].lat],
                ),
            );
            map.fitBounds(bounds, { padding: 72, maxZoom: 14, duration: 0 });
        }

        return () => {
            markers.forEach((marker) => marker.remove());
            map.remove();
        };
    }, [stations, navigate]);

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

function escapeHtml(value: string): string {
    return value.replace(
        /[&<>"']/g,
        (c) =>
            ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c] as string,
    );
}
