import { ApiStation } from './api';
import { Station } from './types';

/**
 * Fallback stations rendered when the discovery API is unreachable, so the UI
 * stays explorable without a running backend.
 */
export const demoStations: Station[] = [
    {
        id: 'long-beach-central',
        name: 'Long Beach Central',
        address: '100 Ocean Blvd, Long Beach, CA',
        lat: 33.7701,
        lng: -118.1937,
        connectors: [
            { id: 'lb-ccs-1', type: 'CCS', power: 150, price: 0.42, available: true },
            { id: 'lb-j1772-2', type: 'J1772', power: 11, price: 0.29, available: true },
        ],
    },
    {
        id: 'shoreline-fast',
        name: 'Shoreline Fast Charge',
        address: '200 Aquarium Way, Long Beach, CA',
        lat: 33.7629,
        lng: -118.1928,
        connectors: [{ id: 'sl-ccs-1', type: 'CCS', power: 250, price: 0.49, available: false }],
    },
];

export function normalize(station: ApiStation): Station {
    return {
        id: station.id,
        name: station.name,
        address: station.address || 'Address unavailable',
        lat: station.latitude ?? station.lat ?? 0,
        lng: station.longitude ?? station.lng ?? 0,
        connectors: station.connectors.map((connector) => ({
            id: connector.id,
            type: connector.type,
            power: connector.powerKw ?? connector.power ?? 0,
            price: connector.ratePerKwh ?? connector.price ?? 0,
            available: connector.available,
        })),
    };
}

/**
 * Stations most recently returned by the API. Reservation and session screens
 * only receive connector ids, so they look up the surrounding station here
 * rather than refetching the whole catalogue.
 */
let cache: Station[] = demoStations;

export function cachedStations(): Station[] {
    return cache;
}

export function cacheStations(stations: Station[]): void {
    cache = stations;
}

export function cacheStation(station: Station): void {
    cache = [...cache.filter((s) => s.id !== station.id), station];
}

export function findStation(id: string): Station | undefined {
    return cache.find((s) => s.id === id) ?? demoStations.find((s) => s.id === id);
}

export function findByConnector(connectorId: string | undefined): Station {
    return cache.find((s) => s.connectors.some((c) => c.id === connectorId)) ?? demoStations[0];
}

export function statusMessage(error: unknown): string {
    return error instanceof Error ? error.message : 'Something went wrong. Please try again.';
}
