import { api } from './api';
import { Application } from '../types';

export const applicationsAPI = {
  list: () => api.get<Application[]>('/applications').then((r) => r.data),
  create: (jobId: number) => api.post<Application>('/applications', { jobId }).then((r) => r.data),
  get: (id: number) => api.get<Application>(`/applications/${id}`).then((r) => r.data),
  updateStatus: (id: number, status: string) => api.put<Application>(`/applications/${id}/status`, { status }).then((r) => r.data),
  events: (id: number) => api.get(`/applications/${id}/events`).then((r) => r.data),
};
