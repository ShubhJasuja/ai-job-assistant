import axios, { AxiosInstance, AxiosError } from 'axios';

const API_BASE = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080';

export const api: AxiosInstance = axios.create({
  baseURL: `${API_BASE}/api/v1`,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token && config.headers) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (err: AxiosError) => {
    const msg = (err.response?.data as { message?: string })?.message || err.message || 'Unknown error';
    console.error('API error:', msg, err.response?.status);
    return Promise.reject(err);
  }
);
