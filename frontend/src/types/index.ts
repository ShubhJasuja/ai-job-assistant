export interface Job {
  id: number;
  title: string;
  company: string;
  location: string;
  remoteStatus: string;
  description: string;
  skills: string[];
  source: string;
  postedAt: string;
  isActive: boolean;
  url?: string;
}

export interface JobMatch {
  id: number;
  job: Job;
  score: number;
  explanation: string;
}

export interface Application {
  id: number;
  userId: number;
  jobId: number;
  status: string;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Resume {
  id: number;
  userId: number;
  name: string;
  content: string;
  versions?: Resume[];
}

export interface UserProfile {
  id: number;
  email: string;
  name?: string;
}

export interface AuthResponse {
  token: string;
}

export interface JobStats {
  total: number;
  newJobs: number;
  shortlisted: number;
  readyToApply: number;
  applied: number;
  offers: number;
}
