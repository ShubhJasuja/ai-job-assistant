import { api } from './api';
import { Job, JobMatch, JobStats } from '../types';

export interface JobSearchFilters {
  keyword?: string;
  company?: string;
  location?: string;
  remote?: boolean;
  employmentType?: string;
  skill?: string;
  state?: string;
  active?: boolean;
  offset?: number;
  limit?: number;
}

export type JobUserState = 'INTERESTED' | 'NOT_INTERESTED' | 'APPLIED' | 'NONE';

export interface JobRefreshSummary {
  fetched: number;
  added: number;
  updated: number;
  skipped: number;
  sources: { source: string; successful: boolean }[];
}

export interface JobSearchSuggestion {
  value: string;
  category: 'ROLE' | 'TECHNOLOGY' | 'COMPANY' | 'LOCATION';
  matches: number;
}

export interface JobSearchPage {
  items: Job[];
  totalElements: number;
  hasNext: boolean;
}

export const jobsAPI = {
  list: (filters?: JobSearchFilters) => api.get<Job[]>('/jobs', { params: filters }).then((r) => r.data),
  discover: (filters?: JobSearchFilters) => api.get<Job[]>('/discovery', { params: filters }).then((r) => r.data),
  searchPage: (filters?: JobSearchFilters) => api.get<JobSearchPage>('/jobs/search', { params: filters }).then((r) => r.data),
  discoverPage: (filters?: JobSearchFilters) => api.get<JobSearchPage>('/discovery/page', { params: filters }).then((r) => r.data),
  suggestions: (query: string, field: 'keyword' | 'location') =>
    api.get<JobSearchSuggestion[]>('/jobs/suggestions', { params: { query, field } }).then((r) => r.data),
  get: (id: number) => api.get<Job>(`/jobs/${id}`).then((r) => r.data),
  stats: () => api.get<JobStats>('/applications/stats').then((r) => r.data),
  create: (job: {
    title: string;
    company: string;
    location?: string;
    remoteStatus?: string;
    employmentType?: string;
    description?: string;
    applicationUrl?: string;
    skills?: string[];
  }) => api.post<Job>('/jobs', job).then((r) => r.data),
  updateState: (id: number, state: JobUserState) =>
    api.put<Job>(`/discovery/${id}/state`, { state }).then((r) => r.data),
  refreshSources: () =>
    api.post<JobRefreshSummary>('/job-sources/refresh', undefined, { timeout: 120000 }).then((r) => r.data),
  authenticate: (email: string, password: string, register: boolean) =>
    api.post<{ accessToken: string; refreshToken?: string }>(`/auth/${register ? 'register' : 'login'}`, { email, password }).then((r) => r.data),
};

export const matchAPI = {
  recalc: () => api.post<JobMatch[]>('/match/recalc').then((r) => r.data),
  recalcMe: () => api.post<JobMatch[]>('/match/recalc/me').then((r) => r.data),
};
