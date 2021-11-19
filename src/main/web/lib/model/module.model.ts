import { Moment } from 'moment';

export interface Module {
    id: number;
    name: string;
    basePackage: string;
    version: string;
    description: string;
    active: boolean;
    artifact: string;
    buildTime: Date;
    umdLocation?: string;
}

export interface ModuleUpdate {
    id?: number;
    name?: string;
    version?: string;
    path?: string;
    install?: boolean;
    uninstall?: boolean;
    buildTime: Moment;
}
