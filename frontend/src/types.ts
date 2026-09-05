export type Connector = {
    id: string;
    type: string;
    power: number;
    price: number;
    available: boolean;
};

export type Station = {
    id: string;
    name: string;
    address: string;
    lat: number;
    lng: number;
    connectors: Connector[];
};
