export interface Job {
  id: number;
  title: string;
  company: string;
  location: string;
  remoteStatus?: string | null;
  employmentType?: string | null;
  description: string;
  source?: string | null;
  postedDate?: string | null;
  ingestedAt?: string | null;
  isActive?: boolean;
  applicationUrl?: string | null;
}

export interface JobMatch {
  id: number;
  job: Job;
  score: number;
  explanation: string;
}

export interface Application {
  id: number;
  jobId: number;
  status: string;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
  jobTitle: string;
  company: string;
  location?: string;
  applicationUrl?: string;
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
  accessToken: string;
  refreshToken?: string;
}

export interface JobStats {
  total: number;
  newJobs: number;
  shortlisted: number;
  readyToApply: number;
  applied: number;
  offers: number;
}
