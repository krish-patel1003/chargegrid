import { accessToken } from './auth/oidc';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088').replace(
    /\/$/,
    '',
);

/** Raised when the session is gone, so callers can send the driver back to sign in. */
export class UnauthorizedError extends Error {
    constructor() {
        super('Your session has expired. Please sign in again.');
    }
}

export type ApiConnector = {
    id: string;
    type: string;
    power?: number;
    powerKw?: number;
    price?: number;
    ratePerKwh?: number;
    available: boolean;
};

export type ApiStation = {
    id: string;
    name: string;
    address?: string;
    lat?: number;
    lng?: number;
    latitude?: number;
    longitude?: number;
    connectors: ApiConnector[];
};

export type Reservation = {
    id: string;
    stationId: string;
    connectorId: string;
    ownerId: string;
    status: string;
    expiresAt: string;
    sessionId?: string | null;
};

export type Session = {
    id: string;
    reservationId: string;
    stationId: string;
    connectorId: string;
    ownerId: string;
    status: string;
    startedAt: string;
    meterKwh: number;
    cost: number;
};

export type Simulator = {
    stationId: string;
    connectors: { id: string; type: string; ratePerKwh: number }[];
    reservations: {
        id: string;
        connectorId: string;
        ownerId: string;
        status: string;
        startCode: string;
    }[];
    sessions: {
        id: string;
        connectorId: string;
        ownerId: string;
        stopCode: string;
        meterKwh: number;
        cost: number;
    }[];
};

async function request<T>(path: string, options: RequestInit = {}, admin = false): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set('Content-Type', 'application/json');

    // The caller identity is never sent from here. The gateway derives it from
    // the access token and forwards it downstream, so a browser cannot claim to
    // be another driver.
    const token = await accessToken();
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }
    if (admin) {
        headers.set('X-Admin-Key', import.meta.env.VITE_ADMIN_KEY || 'demo-admin-key');
    }

    const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
    if (response.status === 401) {
        throw new UnauthorizedError();
    }
    if (!response.ok) {
        throw new Error((await response.text()) || `Request failed (${response.status})`);
    }
    return response.json() as Promise<T>;
}

const post = <T>(path: string, body?: unknown, admin = false) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }, admin);

export const api = {
    nearby: (latitude = 33.7701, longitude = -118.1937) =>
        request<ApiStation[]>(`/api/stations/nearby?latitude=${latitude}&longitude=${longitude}`),
    station: (id: string) => request<ApiStation>(`/api/stations/${id}`),
    reserve: (connectorId: string) => post<Reservation>('/api/reservations', { connectorId }),
    reservation: (id: string) => request<Reservation>(`/api/reservations/${id}`),
    verifyStart: (id: string, code: string) =>
        post<Session>(`/api/reservations/${id}/verify-start`, { code }),
    session: (id: string) => request<Session>(`/api/sessions/${id}`),
    sessions: () => request<Session[]>('/api/sessions'),
    verifyStop: (id: string, code: string) =>
        post<Session>(`/api/sessions/${id}/verify-stop`, { code }),
    simulator: (stationId: string) =>
        request<Simulator>(`/api/admin/stations/${stationId}/simulator`, {}, true),
    meter: (sessionId: string, kwh: number) =>
        post<Session>(`/api/admin/sessions/${sessionId}/meter`, { kwh }, true),
};
