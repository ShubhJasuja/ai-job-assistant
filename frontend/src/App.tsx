import { FormEvent, KeyboardEvent, useEffect, useMemo, useRef, useState } from 'react';
import { applicationsAPI } from './services/applications';
import { JobSearchSuggestion, JobUserState, jobsAPI } from './services/jobs';
import { Application, Job } from './types';
import './styles.css';

type View = 'Overview' | 'Discover jobs' | 'Saved jobs' | 'Applications';

const navigation: { label: View; icon: string }[] = [
  { label: 'Overview', icon: '◫' },
  { label: 'Discover jobs', icon: '⌕' },
  { label: 'Saved jobs', icon: '♡' },
  { label: 'Applications', icon: '▤' },
];

const JOBS_PAGE_SIZE = 24;

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
  const [totalJobCount, setTotalJobCount] = useState(0);
  const [hasMoreJobs, setHasMoreJobs] = useState(false);
  const [applications, setApplications] = useState<Application[]>([]);
  const [applicationsError, setApplicationsError] = useState('');
  const [applicationsLoading, setApplicationsLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [query, setQuery] = useState('');
  const [keywordSuggestions, setKeywordSuggestions] = useState<JobSearchSuggestion[]>([]);
  const [locationSuggestions, setLocationSuggestions] = useState<JobSearchSuggestion[]>([]);
  const [activeSuggestionField, setActiveSuggestionField] = useState<'keyword' | 'location' | null>(null);
  const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1);
  const [companyQuery, setCompanyQuery] = useState('');
  const [locationQuery, setLocationQuery] = useState('');
  const [skillQuery, setSkillQuery] = useState('');
  const [stateFilter, setStateFilter] = useState('ALL');
  const [remoteOnly, setRemoteOnly] = useState(false);
  const [authToken, setAuthToken] = useState(() => localStorage.getItem('jwt_token'));
  const [pendingState, setPendingState] = useState<JobUserState | null>(null);
  const [expandedJobIds, setExpandedJobIds] = useState<number[]>([]);
  const [manualJobOpen, setManualJobOpen] = useState(false);
  const [manualJobBusy, setManualJobBusy] = useState(false);
  const [refreshBusy, setRefreshBusy] = useState(false);
  const [manualJobError, setManualJobError] = useState('');
  const jobRequestSequence = useRef(0);
  const [manualJob, setManualJob] = useState({
    title: '',
    company: '',
    location: '',
    remoteStatus: '',
    employmentType: '',
    applicationUrl: '',
    skills: '',
    description: '',
  });
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
  const token = authToken;

  async function loadJobs(append = false) {
    const requestSequence = ++jobRequestSequence.current;
    if (append) setLoadingMore(true);
    else {
      setLoading(true);
      setLoadingMore(false);
      setLoadError('');
    }
    try {
      const filters = {
        keyword: query.trim() || undefined,
        company: companyQuery.trim() || undefined,
        location: locationQuery.trim() || undefined,
        skill: skillQuery.trim() || undefined,
        remote: remoteOnly ? true : undefined,
        state: view === 'Saved jobs' && token ? 'SAVED' : stateFilter,
        offset: append ? jobs.length : 0,
        limit: JOBS_PAGE_SIZE,
      };
      const result = token
        ? await jobsAPI.discoverPage(filters)
        : await jobsAPI.searchPage({ ...filters, active: true });
      if (requestSequence !== jobRequestSequence.current) return;
      setJobs((current) => append ? [...current, ...result.items] : result.items);
      setTotalJobCount(result.totalElements);
      setHasMoreJobs(result.hasNext);
      if (token) {
        const pageSavedIds = result.items.filter((job) => job.userState === 'INTERESTED').map((job) => job.id);
        setSavedIds((current) => Array.from(new Set([...current, ...pageSavedIds])));
      }
    } catch (error) {
      if (requestSequence !== jobRequestSequence.current) return;
      if (append) setToast(getErrorMessage(error));
      else setLoadError(getErrorMessage(error));
    } finally {
      if (requestSequence === jobRequestSequence.current) {
        if (append) setLoadingMore(false);
        else setLoading(false);
      }
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

  async function refreshJobSources() {
    if (!token) {
      openAuth();
      return;
    }
    setRefreshBusy(true);
    try {
      const summary = await jobsAPI.refreshSources();
      await loadJobs();
      const failedSources = summary.sources.filter((source) => !source.successful).map((source) => source.source);
      setToast(failedSources.length
        ? `Some sources were unavailable: ${failedSources.join(', ')}`
        : `Job refresh complete: ${summary.added} new, ${summary.updated} updated`);
    } catch (error) {
      setToast(getErrorMessage(error));
    } finally {
      setRefreshBusy(false);
    }
  }

  useEffect(() => {
    const timeout = window.setTimeout(() => void loadJobs(), 250);
    return () => window.clearTimeout(timeout);
  }, [authToken, companyQuery, locationQuery, query, remoteOnly, skillQuery, stateFilter, view]);

  useEffect(() => {
    const value = query.trim();
    if (!value) {
      setKeywordSuggestions([]);
      return;
    }
    let cancelled = false;
    const timeout = window.setTimeout(() => {
      void jobsAPI.suggestions(value, 'keyword')
        .then((result) => { if (!cancelled) setKeywordSuggestions(result); })
        .catch((error) => { if (!cancelled) console.error('Could not load search suggestions:', error); });
    }, 160);
    return () => {
      cancelled = true;
      window.clearTimeout(timeout);
    };
  }, [query]);

  useEffect(() => {
    const value = locationQuery.trim();
    if (!value) {
      setLocationSuggestions([]);
      return;
    }
    let cancelled = false;
    const timeout = window.setTimeout(() => {
      void jobsAPI.suggestions(value, 'location')
        .then((result) => { if (!cancelled) setLocationSuggestions(result); })
        .catch((error) => { if (!cancelled) console.error('Could not load location suggestions:', error); });
    }, 160);
    return () => {
      cancelled = true;
      window.clearTimeout(timeout);
    };
  }, [locationQuery]);

  useEffect(() => {
    if (token) void loadApplications();
    else setApplications([]);
  }, [token]);

  useEffect(() => {
    if (!token) localStorage.setItem('saved_job_ids', JSON.stringify(savedIds));
  }, [savedIds, token]);

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
        [job.title, job.company, job.location, job.description, job.employmentType, ...(job.skills || [])]
          .filter(Boolean)
          .some((part) => part!.toLowerCase().includes(normalizedQuery));
      const matchesRemote =
        !remoteOnly || /remote|hybrid/i.test(`${job.remoteStatus || ''} ${job.location || ''}`);
      const matchesLocalSaved = token || view !== 'Saved jobs' || savedIds.includes(job.id);
      return matchesQuery && matchesRemote && matchesLocalSaved && job.isActive !== false;
    });
  }, [jobs, query, remoteOnly, savedIds, token, view]);

  const remoteCount = jobs.filter((job) => /remote/i.test(`${job.remoteStatus || ''} ${job.location || ''}`)).length;
  const companiesCount = new Set(jobs.map((job) => job.company).filter(Boolean)).size;

  function updateLocalJobState(job: Job, state: JobUserState) {
    setJobs((current) => current.map((item) => item.id === job.id ? { ...item, userState: state } : item));
    if (state === 'INTERESTED') setSavedIds((current) => current.includes(job.id) ? current : [...current, job.id]);
    else setSavedIds((current) => current.filter((id) => id !== job.id));
  }

  async function updateJobState(job: Job, state: JobUserState) {
    if (!token) {
      setSelectedJob(job);
      setPendingState(state);
      setAuthError('');
      setAuthOpen(true);
      return;
    }
    try {
      await jobsAPI.updateState(job.id, state);
      updateLocalJobState(job, state);
      if (state === 'APPLIED') await loadApplications();
      setToast(state === 'INTERESTED' ? 'Saved to your shortlist' : state === 'NOT_INTERESTED' ? 'We’ll show fewer roles like this' : state === 'APPLIED' ? 'Marked as applied and added to your tracker' : 'Removed from your saved roles');
      await loadJobs();
    } catch (error) {
      setToast(getErrorMessage(error));
    }
  }

  function toggleSaved(job: Job) {
    const isSaved = savedIds.includes(job.id);
    if (!token) {
      setSavedIds((current) => isSaved ? current.filter((id) => id !== job.id) : [...current, job.id]);
      setToast(isSaved ? 'Removed from your saved jobs' : 'Saved to your shortlist');
      return;
    }

    void updateJobState(job, isSaved ? 'NONE' : 'INTERESTED');
  }

  function chooseSuggestion(field: 'keyword' | 'location', value: string) {
    if (field === 'keyword') setQuery(value);
    else setLocationQuery(value);
    setActiveSuggestionField(null);
    setActiveSuggestionIndex(-1);
  }

  function handleSuggestionKeyDown(
    event: KeyboardEvent<HTMLInputElement>,
    field: 'keyword' | 'location',
    suggestions: JobSearchSuggestion[],
  ) {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveSuggestionField(field);
      setActiveSuggestionIndex((index) => {
        if (suggestions.length === 0) return -1;
        const direction = event.key === 'ArrowDown' ? 1 : -1;
        if (index < 0) return direction > 0 ? 0 : suggestions.length - 1;
        return (index + direction + suggestions.length) % suggestions.length;
      });
    } else if (event.key === 'Enter' && activeSuggestionField === field && activeSuggestionIndex >= 0) {
      event.preventDefault();
      const suggestion = suggestions[activeSuggestionIndex];
      if (suggestion) chooseSuggestion(field, suggestion.value);
    } else if (event.key === 'Escape') {
      setActiveSuggestionField(null);
      setActiveSuggestionIndex(-1);
    }
  }

  async function submitApplication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthError('');
    setAuthBusy(true);
    try {
      const result = await jobsAPI.authenticate(email, password, registerMode);
      localStorage.setItem('jwt_token', result.accessToken);
      if (result.refreshToken) localStorage.setItem('refresh_token', result.refreshToken);
      setAuthToken(result.accessToken);
      setAuthOpen(false);
      setPassword('');
      setToast(registerMode ? 'Your account is ready' : 'Welcome back');
      if (selectedJob && pendingState) {
        const job = selectedJob;
        const state = pendingState;
        setPendingState(null);
        try {
          await jobsAPI.updateState(job.id, state);
          if (state === 'APPLIED') await loadApplications();
          setToast(state === 'INTERESTED' ? 'Saved to your shortlist' : state === 'NOT_INTERESTED' ? 'We’ll show fewer roles like this' : 'Marked as applied and added to your tracker');
        } catch (error) {
          setToast(getErrorMessage(error));
        }
        setSelectedJob(null);
      } else if (selectedJob) {
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

  function openAuth() {
    setSelectedJob(null);
    setPendingState(null);
    setAuthError('');
    setAuthOpen(true);
  }

  async function submitManualJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setManualJobError('');
    if (!token) {
      setManualJobError('Sign in to add a job to the shared job board.');
      setAuthOpen(true);
      return;
    }
    setManualJobBusy(true);
    try {
      await jobsAPI.create({
        title: manualJob.title.trim(),
        company: manualJob.company.trim(),
        location: manualJob.location.trim() || undefined,
        remoteStatus: manualJob.remoteStatus || undefined,
        employmentType: manualJob.employmentType.trim() || undefined,
        applicationUrl: manualJob.applicationUrl.trim() || undefined,
        skills: manualJob.skills.split(',').map((skill) => skill.trim()).filter(Boolean),
        description: manualJob.description.trim() || undefined,
      });
      setManualJobOpen(false);
      setManualJob({ title: '', company: '', location: '', remoteStatus: '', employmentType: '', applicationUrl: '', skills: '', description: '' });
      setToast('Job added to the discovery feed');
      await loadJobs();
    } catch (error) {
      setManualJobError(getErrorMessage(error));
    } finally {
      setManualJobBusy(false);
    }
  }

  function toggleExplanation(jobId: number) {
    setExpandedJobIds((current) => current.includes(jobId)
      ? current.filter((id) => id !== jobId)
      : [...current, jobId]);
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
              aria-current={view === item.label ? 'page' : undefined}
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
                  <div className="stat-value">{loading ? '—' : totalJobCount}</div>
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
              <div className="section-heading-actions">
                {view !== 'Applications' && token && <button className="secondary-button" onClick={() => void refreshJobSources()} disabled={refreshBusy}>{refreshBusy ? 'Refreshing…' : '↻ Refresh jobs'}</button>}
                {view !== 'Applications' && <button className="secondary-button add-job-button" onClick={() => setManualJobOpen(true)}>＋ Add a job</button>}
                {view !== 'Discover jobs' && <button className="text-link" onClick={() => setView('Discover jobs')}>Browse all roles <span>↗</span></button>}
              </div>
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
                  <div className="autocomplete-field search-autocomplete">
                    <label className="search-box">
                      <span className="search-icon">⌕</span>
                      <input
                        role="combobox"
                        aria-label="Search jobs by role, technology, or company"
                        aria-autocomplete="list"
                        aria-expanded={activeSuggestionField === 'keyword' && keywordSuggestions.length > 0}
                        aria-controls="keyword-suggestions"
                        aria-activedescendant={activeSuggestionField === 'keyword' && activeSuggestionIndex >= 0
                          ? `keyword-suggestion-${activeSuggestionIndex}` : undefined}
                        value={query}
                        onFocus={() => { setActiveSuggestionField('keyword'); setActiveSuggestionIndex(-1); }}
                        onBlur={() => { setActiveSuggestionField(null); setActiveSuggestionIndex(-1); }}
                        onChange={(event) => { setQuery(event.target.value); setKeywordSuggestions([]); setActiveSuggestionIndex(-1); }}
                        onKeyDown={(event) => handleSuggestionKeyDown(event, 'keyword', keywordSuggestions)}
                        placeholder="Search roles, technologies, companies..."
                      />
                      {query && <button type="button" aria-label="Clear search" onClick={() => { setQuery(''); setActiveSuggestionIndex(-1); }}>×</button>}
                    </label>
                    {activeSuggestionField === 'keyword' && keywordSuggestions.length > 0 && (
                      <div className="suggestion-list" id="keyword-suggestions" role="listbox" aria-label="Search suggestions">
                        {keywordSuggestions.map((suggestion, index) => (
                          <button
                            type="button"
                            role="option"
                            id={`keyword-suggestion-${index}`}
                            aria-selected={index === activeSuggestionIndex}
                            className="suggestion-option"
                            key={`${suggestion.category}-${suggestion.value}`}
                            onMouseDown={(event) => event.preventDefault()}
                            onClick={() => chooseSuggestion('keyword', suggestion.value)}
                          >
                            <span>{suggestion.value}</span>
                            <small>{suggestion.category.toLowerCase()}</small>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                  <button className={`filter-button ${remoteOnly ? 'filter-active' : ''}`} onClick={() => setRemoteOnly((value) => !value)}>
                    <span>☷</span> Remote friendly <span className="filter-toggle">{remoteOnly ? '✓' : '+'}</span>
                  </button>
                  {token && <label className="state-filter-label">Show
                    <select value={stateFilter} onChange={(event) => setStateFilter(event.target.value)}>
                      <option value="ALL">All jobs</option>
                      <option value="INTERESTED">Interested</option>
                      <option value="NOT_INTERESTED">Not interested</option>
                      <option value="APPLIED">Applied</option>
                    </select>
                  </label>}
                  <div className="results-count">
                    {loading ? 'Finding roles...' : totalJobCount > filteredJobs.length
                      ? `${filteredJobs.length} of ${totalJobCount} roles`
                      : `${totalJobCount} ${totalJobCount === 1 ? 'role' : 'roles'}`}
                  </div>
                </div>
                <div className="discovery-filters">
                  <input aria-label="Filter by company" value={companyQuery} onChange={(event) => setCompanyQuery(event.target.value)} placeholder="Company" />
                  <div className="autocomplete-field location-autocomplete">
                    <input
                      role="combobox"
                      aria-label="Filter by location"
                      aria-autocomplete="list"
                      aria-expanded={activeSuggestionField === 'location' && locationSuggestions.length > 0}
                      aria-controls="location-suggestions"
                      aria-activedescendant={activeSuggestionField === 'location' && activeSuggestionIndex >= 0
                        ? `location-suggestion-${activeSuggestionIndex}` : undefined}
                      value={locationQuery}
                      onFocus={() => { setActiveSuggestionField('location'); setActiveSuggestionIndex(-1); }}
                      onBlur={() => { setActiveSuggestionField(null); setActiveSuggestionIndex(-1); }}
                      onChange={(event) => { setLocationQuery(event.target.value); setLocationSuggestions([]); setActiveSuggestionIndex(-1); }}
                      onKeyDown={(event) => handleSuggestionKeyDown(event, 'location', locationSuggestions)}
                      placeholder="Location"
                    />
                    {activeSuggestionField === 'location' && locationSuggestions.length > 0 && (
                      <div className="suggestion-list" id="location-suggestions" role="listbox" aria-label="Location suggestions">
                        {locationSuggestions.map((suggestion, index) => (
                          <button
                            type="button"
                            role="option"
                            id={`location-suggestion-${index}`}
                            aria-selected={index === activeSuggestionIndex}
                            className="suggestion-option"
                            key={suggestion.value}
                            onMouseDown={(event) => event.preventDefault()}
                            onClick={() => chooseSuggestion('location', suggestion.value)}
                          >
                            <span>{suggestion.value}</span>
                            <small>{suggestion.matches} jobs</small>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                  <input aria-label="Filter by skill" value={skillQuery} onChange={(event) => setSkillQuery(event.target.value)} placeholder="Skill" />
                  {(companyQuery || locationQuery || skillQuery || stateFilter !== 'ALL') &&
                    <button className="clear-filters-button" onClick={() => { setCompanyQuery(''); setLocationQuery(''); setSkillQuery(''); setStateFilter('ALL'); }}>Clear filters</button>}
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
                            {job.skills?.slice(0, 3).map((skill) => <span className="job-tag" key={skill}>{skill}</span>)}
                          </div>
                          <div className="job-card-meta">
                            {job.matchScore !== undefined && job.matchScore !== null && <span className="match-score">{job.matchScore}% match</span>}
                            {job.explanation && <button className="explanation-toggle" onClick={() => toggleExplanation(job.id)}>{expandedJobIds.includes(job.id) ? 'Hide match details' : 'Why this match?'}</button>}
                          </div>
                          {expandedJobIds.includes(job.id) && (
                            <div className="match-explanation">
                              <p>{job.explanation}</p>
                              {Boolean(job.matchedSkills?.length) && <div><strong>Matching skills</strong><div className="job-tags">{job.matchedSkills?.map((skill) => <span className="job-tag tag-green" key={skill}>{skill}</span>)}</div></div>}
                              {Boolean(job.missingSkills?.length) && <div><strong>Skills to build</strong><div className="job-tags">{job.missingSkills?.map((skill) => <span className="job-tag tag-muted" key={skill}>{skill}</span>)}</div></div>}
                              <div className="match-state-actions">
                                <button onClick={() => void updateJobState(job, 'NOT_INTERESTED')}>Not interested</button>
                                <button onClick={() => void updateJobState(job, 'APPLIED')}>Mark as applied</button>
                              </div>
                            </div>
                          )}
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
                {view === 'Overview' && totalJobCount > 4 && (
                  <button className="load-more-button" onClick={() => setView('Discover jobs')}>Explore all {totalJobCount} opportunities <span>→</span></button>
                )}
                {view !== 'Overview' && hasMoreJobs && !loadError && (
                  <button className="load-more-button" onClick={() => void loadJobs(true)} disabled={loadingMore}>
                    {loadingMore ? 'Loading more roles…' : `Show more roles (${filteredJobs.length} of ${totalJobCount})`}
                  </button>
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
            {selectedJob.matchScore !== undefined && selectedJob.matchScore !== null && (
              <section className="detail-match">
                <strong>{selectedJob.matchScore}% match</strong>
                <p>{selectedJob.explanation || 'Your profile has not been matched against this role yet.'}</p>
                {Boolean(selectedJob.matchedSkills?.length) && <div><b>Matched skills:</b> {selectedJob.matchedSkills?.join(', ')}</div>}
                {Boolean(selectedJob.missingSkills?.length) && <div><b>Potential gaps:</b> {selectedJob.missingSkills?.join(', ')}</div>}
              </section>
            )}
            <div className="detail-description">{selectedJob.description || 'No additional description was provided for this opportunity.'}</div>
            <div className="detail-actions">
              <button className="primary-button" onClick={() => void updateJobState(selectedJob, 'APPLIED')}>Mark as applied <span>→</span></button>
              <button className={`secondary-button ${savedIds.includes(selectedJob.id) ? 'saved-secondary' : ''}`} onClick={() => toggleSaved(selectedJob)}>{savedIds.includes(selectedJob.id) ? '♥ Saved' : '♡ Save role'}</button>
              {selectedJob.applicationUrl && <a className="external-apply" href={selectedJob.applicationUrl} target="_blank" rel="noreferrer">Visit application page ↗</a>}
            </div>
            <p className="detail-note">Marking this as applied updates your tracker only. It does not submit an application to the employer.</p>
          </section>
        </div>
      )}

      {manualJobOpen && (
        <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setManualJobOpen(false); }}>
          <section className="manual-job-modal" role="dialog" aria-modal="true" aria-labelledby="manual-job-title">
            <button className="modal-close" aria-label="Close add job form" onClick={() => setManualJobOpen(false)}>×</button>
            <div className="section-kicker">ADD AN OPPORTUNITY</div>
            <h2 id="manual-job-title">Add a job to discover</h2>
            <p>Save a role you found elsewhere and keep it with your research.</p>
            <form className="manual-job-form" onSubmit={(event) => void submitManualJob(event)}>
              <label>Job title<input required value={manualJob.title} onChange={(event) => setManualJob({ ...manualJob, title: event.target.value })} /></label>
              <label>Company<input required value={manualJob.company} onChange={(event) => setManualJob({ ...manualJob, company: event.target.value })} /></label>
              <label>Location<input value={manualJob.location} onChange={(event) => setManualJob({ ...manualJob, location: event.target.value })} /></label>
              <label>Work arrangement
                <select value={manualJob.remoteStatus} onChange={(event) => setManualJob({ ...manualJob, remoteStatus: event.target.value })}>
                  <option value="">Not specified</option><option value="REMOTE">Remote</option><option value="HYBRID">Hybrid</option><option value="ON_SITE">On-site</option>
                </select>
              </label>
              <label>Employment type<input placeholder="Full-time, contract..." value={manualJob.employmentType} onChange={(event) => setManualJob({ ...manualJob, employmentType: event.target.value })} /></label>
              <label>Application URL<input type="url" value={manualJob.applicationUrl} onChange={(event) => setManualJob({ ...manualJob, applicationUrl: event.target.value })} /></label>
              <label className="manual-job-wide">Skills, separated by commas<input value={manualJob.skills} onChange={(event) => setManualJob({ ...manualJob, skills: event.target.value })} /></label>
              <label className="manual-job-wide">Description<textarea rows={4} value={manualJob.description} onChange={(event) => setManualJob({ ...manualJob, description: event.target.value })} /></label>
              {manualJobError && <div className="form-error manual-job-wide">{manualJobError}</div>}
              <div className="manual-job-actions manual-job-wide">
                <button type="button" className="secondary-button" onClick={() => setManualJobOpen(false)}>Cancel</button>
                <button className="primary-button" disabled={manualJobBusy}>{manualJobBusy ? 'Adding job...' : 'Add job'}</button>
              </div>
            </form>
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
            <p>{selectedJob && pendingState ? `Sign in to update your status for ${selectedJob.title}.` : selectedJob ? `Sign in to add ${selectedJob.title} to your tracker.` : 'Sign in to keep your search and applications together.'}</p>
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
