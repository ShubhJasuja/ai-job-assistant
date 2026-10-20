import { api } from './api';
import { Job, JobMatch, JobStats } from '../types';

export const jobsAPI = {
  list: () => api.get<Job[]>('/jobs').then((r) => r.data),
  get: (id: number) => api.get<Job>(`/jobs/${id}`).then((r) => r.data),
  stats: () => api.get<JobStats>('/applications/stats').then((r) => r.data),
};

export const matchAPI = {
  recalc: () => api.post<JobMatch[]>('/match/recalc').then((r) => r.data),
  recalcMe: () => api.post<JobMatch[]>('/match/recalc/me').then((r) => r.data),
};
