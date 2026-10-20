import { FormEvent, useEffect, useMemo, useState } from 'react';
import { applicationsAPI } from './services/applications';
import { jobsAPI } from './services/jobs';
import { Application, Job } from './types';
import './styles.css';

type View = 'Overview' | 'Discover jobs' | 'Saved jobs' | 'Applications';

const navigation: { label: View; icon: string }[] = [
  { label: 'Overview', icon: '◫' },
  { label: 'Discover jobs', icon: '⌕' },
  { label: 'Saved jobs', icon: '♡' },
  { label: 'Applications', icon: '▤' },
];

function getErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.';
}

function formatDate(date?: string | null) {
  if (!date) return 'Recently posted';
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return 'Recently posted';
  const days = Math.max(0, Math.floor((Date.now() - parsed.getTime()) / 86_400_000));
  if (days === 0) return 'Today';
  if (days === 1) return 'Yesterday';
  if (days < 7) return `${days} days ago`;
  return parsed.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

function initials(company?: string) {
  return (company || 'J')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

export default function App() {
  const [view, setView] = useState<View>('Overview');
  const [jobs, setJobs] = useState<Job[]>([]);
  const [applications, setApplications] = useState<Application[]>([]);
  const [applicationsError, setApplicationsError] = useState('');
  const [applicationsLoading, setApplicationsLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [query, setQuery] = useState('');
  const [remoteOnly, setRemoteOnly] = useState(false);
  const [savedIds, setSavedIds] = useState<number[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('saved_job_ids') || '[]') as number[];
    } catch {
      return [];
    }
  });
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);
  const [authOpen, setAuthOpen] = useState(false);
  const [registerMode, setRegisterMode] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [authBusy, setAuthBusy] = useState(false);
  const [toast, setToast] = useState('');
  const token = localStorage.getItem('jwt_token');

  async function loadJobs() {
    setLoading(true);
    setLoadError('');
    try {
      setJobs(await jobsAPI.list());
    } catch (error) {
      setLoadError(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function loadApplications() {
    setApplicationsLoading(true);
    setApplicationsError('');
    try {
      setApplications(await applicationsAPI.list());
    } catch (error) {
      setApplicationsError(getErrorMessage(error));
    } finally {
      setApplicationsLoading(false);
    }
  }

  useEffect(() => {
    void loadJobs();
  }, []);

  useEffect(() => {
    if (token) void loadApplications();
    else setApplications([]);
  }, [token]);

  useEffect(() => {
    localStorage.setItem('saved_job_ids', JSON.stringify(savedIds));
  }, [savedIds]);

  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(''), 3200);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  const filteredJobs = useMemo(() => {
    const source = view === 'Saved jobs' ? jobs.filter((job) => savedIds.includes(job.id)) : jobs;
    const normalizedQuery = query.trim().toLowerCase();
    return source.filter((job) => {
      const matchesQuery =
        !normalizedQuery ||
        [job.title, job.company, job.location, job.description, job.employmentType]
          .filter(Boolean)
          .some((part) => part!.toLowerCase().includes(normalizedQuery));
      const matchesRemote =
        !remoteOnly || /remote|hybrid/i.test(`${job.remoteStatus || ''} ${job.location || ''}`);
      return matchesQuery && matchesRemote && job.isActive !== false;
    });
  }, [jobs, query, remoteOnly, savedIds, view]);

  const remoteCount = jobs.filter((job) => /remote/i.test(`${job.remoteStatus || ''} ${job.location || ''}`)).length;
  const companiesCount = new Set(jobs.map((job) => job.company).filter(Boolean)).size;

  function toggleSaved(job: Job) {
    const isSaved = savedIds.includes(job.id);
    setSavedIds((current) => (isSaved ? current.filter((id) => id !== job.id) : [...current, job.id]));
    setToast(isSaved ? 'Removed from your saved jobs' : 'Saved to your shortlist');
  }

  async function submitApplication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthError('');
    setAuthBusy(true);
    try {
      const result = await jobsAPI.authenticate(email, password, registerMode);
      localStorage.setItem('jwt_token', result.accessToken);
      if (result.refreshToken) localStorage.setItem('refresh_token', result.refreshToken);
      setAuthOpen(false);
      setPassword('');
      setToast(registerMode ? 'Your account is ready' : 'Welcome back');
      if (selectedJob) {
        try {
          await applicationsAPI.create(selectedJob.id);
          await loadApplications();
          setToast('Job added to your application tracker');
        } catch (error) {
          setToast(getErrorMessage(error));
        }
      }
    } catch (error) {
      setAuthError(getErrorMessage(error));
    } finally {
      setAuthBusy(false);
    }
  }

  async function trackJob(job: Job) {
    if (!localStorage.getItem('jwt_token')) {
      setSelectedJob(job);
      setAuthOpen(true);
      return;
    }
    try {
      await applicationsAPI.create(job.id);
      await loadApplications();
      setToast('Job added to your application tracker');
    } catch (error) {
      setToast(getErrorMessage(error));
    }
  }

  function openAuth() {
    setSelectedJob(null);
    setAuthError('');
    setAuthOpen(true);
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#" onClick={() => setView('Overview')} aria-label="Pathwise home">
          <span className="brand-mark"><span /></span>
          <span className="brand-name">pathwise<span>.</span></span>
        </a>

        <div className="workspace-label">WORKSPACE</div>
        <nav className="sidebar-nav" aria-label="Main navigation">
          {navigation.map((item) => (
            <button
              key={item.label}
              className={`nav-item ${view === item.label ? 'active' : ''}`}
              onClick={() => setView(item.label)}
            >
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>
              {item.label === 'Saved jobs' && savedIds.length > 0 && <span className="nav-count">{savedIds.length}</span>}
            </button>
          ))}
        </nav>

        <div className="sidebar-spacer" />
        <div className="sidebar-tip">
          <span className="tip-spark">✳</span>
          <p className="tip-title">A little progress, every day.</p>
          <p className="tip-copy">Your next opportunity is closer than you think.</p>
          <div className="tip-progress"><span style={{ width: `${Math.min(100, jobs.length * 6)}%` }} /></div>
          <span className="tip-caption">{jobs.length} opportunities in your feed</span>
        </div>
        <div className="sidebar-footer">
          <span className="avatar avatar-small">P</span>
          <span className="profile-copy"><strong>{token ? 'Your workspace' : 'Guest workspace'}</strong><small>{token ? 'Account connected' : 'Personal job search'}</small></span>
          <button className="more-button" aria-label="Account options" onClick={token ? () => { localStorage.removeItem('jwt_token'); localStorage.removeItem('refresh_token'); window.location.reload(); } : openAuth}>
            {token ? '↪' : '···'}
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>Workspace</span><span className="crumb-divider">/</span><strong>{view}</strong></div>
          <div className="topbar-actions">
            <span className="today-date">{new Date().toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}</span>
            <span className="topbar-divider" />
            {token ? (
              <button className="user-chip" onClick={() => { localStorage.removeItem('jwt_token'); localStorage.removeItem('refresh_token'); window.location.reload(); }}>
                <span className="avatar avatar-top">Y</span><span>My account</span><span className="chevron">⌄</span>
              </button>
            ) : (
              <button className="sign-in-button" onClick={openAuth}>Sign in <span>↗</span></button>
            )}
          </div>
        </header>

        <div className="page-content">
          {view === 'Overview' && (
            <>
              <section className="welcome-row">
                <div>
                  <div className="eyebrow"><span className="eyebrow-dot" /> YOUR CAREER, IN GOOD COMPANY</div>
                  <h1>Make your next move<br /><em>your best one.</em></h1>
                  <p className="welcome-copy">A calmer, smarter way to find work that fits the life you want.</p>
                </div>
                <div className="welcome-art" aria-hidden="true">
                  <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" />
                  <div className="art-sun" /><div className="art-hill hill-back" /><div className="art-hill hill-front" />
                  <span className="art-star star-one">✳</span><span className="art-star star-two">✦</span>
                  <div className="art-note"><span>✦</span> Your next chapter awaits</div>
                </div>
              </section>

              <section className="stats-grid" aria-label="Job search summary">
                <article className="stat-card">
                  <div className="stat-top"><span className="stat-icon icon-lilac">⌕</span><span className="stat-label">OPEN OPPORTUNITIES</span></div>
                  <div className="stat-value">{loading ? '—' : jobs.length}</div>
                  <div className="stat-foot"><span className="stat-dot dot-violet" /> Roles in your discovery feed</div>
                </article>
                <article className="stat-card">
                  <div className="stat-top"><span className="stat-icon icon-peach">✳</span><span className="stat-label">COMPANIES TO EXPLORE</span></div>
                  <div className="stat-value">{loading ? '—' : companiesCount}</div>
                  <div className="stat-foot"><span className="stat-dot dot-orange" /> Across your current listings</div>
                </article>
                <article className="stat-card">
                  <div className="stat-top"><span className="stat-icon icon-mint">⌁</span><span className="stat-label">REMOTE-FRIENDLY</span></div>
                  <div className="stat-value">{loading ? '—' : remoteCount}</div>
                  <div className="stat-foot"><span className="stat-dot dot-green" /> Roles with location flexibility</div>
                </article>
                <article className="stat-card stat-highlight">
                  <div className="stat-top"><span className="stat-icon icon-white">♡</span><span className="stat-label">YOUR SHORTLIST</span></div>
                  <div className="stat-value">{savedIds.length}</div>
                  <button className="stat-link" onClick={() => setView('Saved jobs')}>Review saved roles <span>→</span></button>
                </article>
              </section>
            </>
          )}

          <section className="jobs-section">
            <div className="section-heading">
              <div>
                <div className="section-kicker">{view === 'Saved jobs' ? 'KEEP YOUR FAVORITES CLOSE' : 'HANDPICKED FOR YOUR NEXT CHAPTER'}</div>
                <h2>{view === 'Saved jobs' ? 'Your shortlist' : view === 'Discover jobs' ? 'Explore opportunities' : view === 'Applications' ? 'Your application journey' : 'A few places to start'}</h2>
                <p>{view === 'Saved jobs' ? 'The roles you have saved for a closer look.' : view === 'Applications' ? 'Keep your applications moving, one thoughtful step at a time.' : 'Thoughtful opportunities for wherever you want to go next.'}</p>
              </div>
              <button className="text-link" onClick={() => setView('Discover jobs')}>Browse all roles <span>↗</span></button>
            </div>

            {view === 'Applications' ? (
              !token ? (
                <div className="empty-state application-empty">
                  <div className="empty-illustration">▤</div>
                  <h3>Your journey starts with one application</h3>
                  <p>Sign in to see your applications, keep track of next steps, and make the process feel a little more manageable.</p>
                  <button className="primary-button" onClick={openAuth}>Sign in to get started <span>→</span></button>
                </div>
              ) : applicationsLoading ? (
                <div className="job-list">
                  {[0, 1].map((item) => <div className="job-skeleton" key={item}><span /><div><i /><i /><i /></div></div>)}
                </div>
              ) : applicationsError ? (
                <div className="empty-state error-state">
                  <div className="empty-illustration">!</div>
                  <h3>We couldn’t load your application tracker</h3>
                  <p>{applicationsError}</p>
                  <button className="secondary-button" onClick={() => void loadApplications()}>Try again <span>↻</span></button>
                </div>
              ) : applications.length === 0 ? (
                <div className="empty-state application-empty">
                  <div className="empty-illustration">▤</div>
                  <h3>Your journey starts with one application</h3>
                  <p>Track roles you’re pursuing, keep your next steps in one place, and make the process feel a little more manageable.</p>
                  <button className="primary-button" onClick={() => setView('Discover jobs')}>Discover roles <span>→</span></button>
                </div>
              ) : (
                <div className="application-list">
                  {applications.map((application) => (
                    <article className="application-card" key={application.id}>
                      <div className="company-mark company-2">{initials(application.company)}</div>
                      <div className="application-main">
                        <strong>{application.jobTitle}</strong>
                        <span>{application.company} <i>·</i> {application.location || 'Location flexible'}</span>
                        {application.notes && <small>{application.notes}</small>}
                      </div>
                      <div className={`application-status status-${application.status.toLowerCase()}`}>{application.status.replace(/_/g, ' ')}</div>
                      <span className="application-date">Added {formatDate(application.createdAt)}</span>
                      {application.applicationUrl && <a className="application-link" href={application.applicationUrl} target="_blank" rel="noreferrer" aria-label={`Open ${application.jobTitle} application page`}>↗</a>}
                    </article>
                  ))}
                </div>
              )
            ) : (
              <>
                <div className="job-toolbar">
                  <label className="search-box">
                    <span className="search-icon">⌕</span>
                    <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search roles, companies, or places..." />
                    {query && <button aria-label="Clear search" onClick={() => setQuery('')}>×</button>}
                  </label>
                  <button className={`filter-button ${remoteOnly ? 'filter-active' : ''}`} onClick={() => setRemoteOnly((value) => !value)}>
                    <span>☷</span> Remote friendly <span className="filter-toggle">{remoteOnly ? '✓' : '+'}</span>
                  </button>
                  <div className="results-count">{loading ? 'Finding roles...' : `${filteredJobs.length} ${filteredJobs.length === 1 ? 'role' : 'roles'}`}</div>
                </div>

                {loadError ? (
                  <div className="empty-state error-state">
                    <div className="empty-illustration">!</div>
                    <h3>We couldn’t load your opportunities</h3>
                    <p>{loadError.includes('Network Error') ? 'Check that the job assistant API is running, then try again.' : loadError}</p>
                    <button className="secondary-button" onClick={() => void loadJobs()}>Try again <span>↻</span></button>
                  </div>
                ) : loading ? (
                  <div className="job-list">
                    {[0, 1, 2].map((item) => <div className="job-skeleton" key={item}><span /><div><i /><i /><i /></div></div>)}
                  </div>
                ) : filteredJobs.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-illustration">{view === 'Saved jobs' ? '♡' : '⌕'}</div>
                    <h3>{view === 'Saved jobs' ? 'Your shortlist is a blank canvas' : 'No roles found just yet'}</h3>
                    <p>{view === 'Saved jobs' ? 'Save interesting opportunities as you browse, and they’ll be waiting here.' : 'Try another search, or check back soon as new roles are added.'}</p>
                    {view === 'Saved jobs' && <button className="secondary-button" onClick={() => setView('Discover jobs')}>Explore opportunities <span>→</span></button>}
                  </div>
                ) : (
                  <div className="job-list">
                    {filteredJobs.slice(0, view === 'Overview' ? 4 : undefined).map((job, index) => (
                      <article className="job-card" key={job.id}>
                        <div className={`company-mark company-${index % 5}`}>{initials(job.company)}</div>
                        <div className="job-main">
                          <div className="job-title-row">
                            <button className="job-title" onClick={() => setSelectedJob(job)}>{job.title || 'Opportunity'}</button>
                            <span className="job-posted">{formatDate(job.postedDate || job.ingestedAt)}</span>
                          </div>
                          <div className="job-company">{job.company || 'Company'} <span>·</span> {job.location || job.remoteStatus || 'Location flexible'}</div>
                          <div className="job-tags">
                            {job.employmentType && <span className="job-tag">{job.employmentType}</span>}
                            {job.remoteStatus && <span className="job-tag tag-green">{job.remoteStatus.replace(/_/g, ' ')}</span>}
                            {job.source && <span className="job-tag tag-muted">via {job.source}</span>}
                          </div>
                        </div>
                        <div className="job-actions">
                          <button className={`save-button ${savedIds.includes(job.id) ? 'is-saved' : ''}`} aria-label={savedIds.includes(job.id) ? 'Remove saved role' : 'Save role'} onClick={() => toggleSaved(job)}>
                            {savedIds.includes(job.id) ? '♥' : '♡'}
                          </button>
                          <button className="view-job-button" onClick={() => setSelectedJob(job)}>View role <span>↗</span></button>
                        </div>
                      </article>
                    ))}
                  </div>
                )}
                {view === 'Overview' && filteredJobs.length > 4 && (
                  <button className="load-more-button" onClick={() => setView('Discover jobs')}>See all {filteredJobs.length} opportunities <span>→</span></button>
                )}
              </>
            )}
          </section>

          <section className="bottom-banner">
            <div className="banner-icon">✦</div>
            <div><strong>Better applications begin with a little preparation.</strong><p>Save the roles you love. We’ll help you keep your search organized.</p></div>
            <button onClick={() => setView('Saved jobs')}>View shortlist <span>→</span></button>
          </section>
          <footer className="page-footer"><span>Made for the work you want.</span><span>Pathwise <b>·</b> Your job search, with a little more clarity.</span></footer>
        </div>
      </main>

      {selectedJob && (
        <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedJob(null); }}>
          <section className="detail-modal" role="dialog" aria-modal="true" aria-labelledby="detail-title">
            <button className="modal-close" aria-label="Close job details" onClick={() => setSelectedJob(null)}>×</button>
            <div className="detail-company-mark">{initials(selectedJob.company)}</div>
            <div className="section-kicker">AN OPPORTUNITY TO EXPLORE</div>
            <h2 id="detail-title">{selectedJob.title || 'Opportunity'}</h2>
            <p className="detail-subtitle">{selectedJob.company || 'Company'} <span>·</span> {selectedJob.location || selectedJob.remoteStatus || 'Location flexible'}</p>
            <div className="job-tags detail-tags">
              {selectedJob.employmentType && <span className="job-tag">{selectedJob.employmentType}</span>}
              {selectedJob.remoteStatus && <span className="job-tag tag-green">{selectedJob.remoteStatus.replace(/_/g, ' ')}</span>}
              <span className="job-tag tag-muted">{formatDate(selectedJob.postedDate || selectedJob.ingestedAt)}</span>
            </div>
            <div className="detail-description">{selectedJob.description || 'No additional description was provided for this opportunity.'}</div>
            <div className="detail-actions">
              <button className="primary-button" onClick={() => void trackJob(selectedJob)}>Add to my tracker <span>→</span></button>
              <button className={`secondary-button ${savedIds.includes(selectedJob.id) ? 'saved-secondary' : ''}`} onClick={() => toggleSaved(selectedJob)}>{savedIds.includes(selectedJob.id) ? '♥ Saved' : '♡ Save role'}</button>
              {selectedJob.applicationUrl && <a className="external-apply" href={selectedJob.applicationUrl} target="_blank" rel="noreferrer">Visit application page ↗</a>}
            </div>
            <p className="detail-note">Your application is only submitted when you choose to continue on the employer’s site.</p>
          </section>
        </div>
      )}

      {authOpen && (
        <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setAuthOpen(false); }}>
          <section className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title">
            <button className="modal-close" aria-label="Close sign in" onClick={() => setAuthOpen(false)}>×</button>
            <span className="brand-mark auth-mark"><span /></span>
            <div className="section-kicker">{registerMode ? 'START YOUR NEXT CHAPTER' : 'GOOD TO HAVE YOU BACK'}</div>
            <h2 id="auth-title">{registerMode ? 'Create your space' : 'Welcome back'}</h2>
            <p>{selectedJob ? `Sign in to add ${selectedJob.title} to your tracker.` : 'Sign in to keep your search and applications together.'}</p>
            <form className="auth-form" onSubmit={(event) => void submitApplication(event)}>
              <label>Email address<input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></label>
              <label>Password<input type="password" autoComplete={registerMode ? 'new-password' : 'current-password'} required minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" /></label>
              {authError && <div className="form-error">{authError}</div>}
              <button className="primary-button auth-submit" disabled={authBusy}>{authBusy ? 'One moment...' : registerMode ? 'Create account' : 'Sign in'} <span>→</span></button>
            </form>
            <div className="auth-switch">{registerMode ? 'Already have an account?' : 'New to Pathwise?'} <button onClick={() => { setRegisterMode((mode) => !mode); setAuthError(''); }}>{registerMode ? 'Sign in' : 'Create an account'}</button></div>
          </section>
        </div>
      )}

      {toast && <div className="toast-message"><span>✦</span>{toast}</div>}
    </div>
  );
}
