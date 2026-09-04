const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088').replace(/\/$/, '');
const USER_ID = 'demo-driver';

export type ApiConnector = { id: string; type: string; power?: number; powerKw?: number; price?: number; ratePerKwh?: number; available: boolean };
export type ApiStation = { id: string; name: string; address?: string; lat?: number; lng?: number; latitude?: number; longitude?: number; connectors: ApiConnector[] };
export type Reservation = { id: string; connectorId: string; ownerId: string; status: string; expiresAt: string; sessionId?: string | null };
export type Session = { id: string; reservationId: string; connectorId: string; ownerId: string; status: string; startedAt: string; meterKwh: number; cost: number };
export type Simulator = { stationId: string; connectors: { id: string; type: string; ratePerKwh: number }[]; reservations: { id: string; connectorId: string; ownerId: string; status: string; startCode: string }[]; sessions: { id: string; connectorId: string; ownerId: string; stopCode: string; meterKwh: number; cost: number }[] };

async function request<T>(path: string, options: RequestInit = {}, admin = false): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  headers.set('X-User-Id', USER_ID);
  if (admin) headers.set('X-Admin-Key', import.meta.env.VITE_ADMIN_KEY || 'demo-admin-key');
  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  if (!response.ok) throw new Error((await response.text()) || `Request failed (${response.status})`);
  return response.json() as Promise<T>;
}

export const api = {
  nearby: (latitude = 33.7701, longitude = -118.1937) => request<ApiStation[]>(`/api/stations/nearby?latitude=${latitude}&longitude=${longitude}`),
  station: (id: string) => request<ApiStation>(`/api/stations/${id}`),
  reservation: (id: string) => request<Reservation>(`/api/reservations/${id}`),
  reserve: (connectorId: string) => request<Reservation>('/api/reservations', { method: 'POST', body: JSON.stringify({ connectorId }) }),
  verifyStart: (id: string, code: string) => request<Session>(`/api/reservations/${id}/verify-start`, { method: 'POST', body: JSON.stringify({ code }) }),
  session: (id: string) => request<Session>(`/api/sessions/${id}`),
  verifyStop: (id: string, code: string) => request<Session>(`/api/sessions/${id}/verify-stop`, { method: 'POST', body: JSON.stringify({ code }) }),
  simulator: (stationId: string) => request<Simulator>(`/api/admin/stations/${stationId}/simulator`, {}, true),
  meter: (sessionId: string, kwh: number) => request<Session>(`/api/admin/sessions/${sessionId}/meter`, { method: 'POST', body: JSON.stringify({ kwh }) }, true),
};
