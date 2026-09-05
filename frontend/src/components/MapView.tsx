import { useEffect, useRef } from 'react';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { Station } from '../types';

const MAP_STYLE = 'https://demotiles.maplibre.org/style.json';
const MARKER_COLOR = '#a4e96c';

export function MapView({ stations }: { stations: Station[] }) {
    const container = useRef<HTMLDivElement>(null);

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
        stations.forEach((station) =>
            new maplibregl.Marker({ color: MARKER_COLOR })
                .setLngLat([station.lng, station.lat])
                .setPopup(new maplibregl.Popup().setText(station.name))
                .addTo(map),
        );
        return () => map.remove();
    }, [stations]);

    return <div className="map" ref={container} />;
}
