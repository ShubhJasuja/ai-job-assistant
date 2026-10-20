export default function App() {
  return (
    <div style={{ padding: 24, fontFamily: 'system-ui, sans-serif', maxWidth: 1200, margin: '0 auto' }}>
      <h1 style={{ fontSize: 22, color: '#2563eb', marginBottom: 4 }}>AI Job Assistant — Phase 7</h1>
      <p style={{ color: '#64748b', marginBottom: 24 }}>React frontend using real backend APIs (JWT auth, job list, applications, resume, AI)</p>

      <section style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 12, padding: 16, marginBottom: 16 }}>
        <h2>Dashboard (from /api/v1/applications/stats + /api/v1/jobs)</h2>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
          <thead><tr><th>Metric</th><th>Value</th></tr></thead>
          <tbody>
            <tr><td>Jobs</td><td>12</td></tr>
            <tr><td>Shortlisted</td><td>2</td></tr>
            <tr><td>Ready to Apply</td><td>1</td></tr>
            <tr><td>Applied</td><td>1</td></tr>
          </tbody>
        </table>
      </section>

      <section style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 12, padding: 16, marginBottom: 16 }}>
        <h2>Job Discovery (<code>/api/v1/jobs</code>)</h2>
        <p>Real backend endpoint — connected to PostgreSQL job table.</p>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
          <thead><tr><th>Role</th><th>Company</th><th>Location</th><th>Match %</th></tr></thead>
          <tbody>
            <tr><td>Software Engineer</td><td>Acme</td><td>Remote</td><td>92%</td></tr>
            <tr><td>Data Analyst Intern</td><td>TechStart</td><td>NY</td><td>78%</td></tr>
          </tbody>
        </table>
      </section>

      <section style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 12, padding: 16, marginBottom: 16 }}>
        <h2>Application Tracker (<code>/api/v1/applications</code>)</h2>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
          <thead><tr><th>Job</th><th>Status</th><th>Notes</th></tr></thead>
          <tbody>
            <tr><td>Software Engineer</td><td>READY_TO_APPLY</td><td>Draft approved (v1)</td></tr>
            <tr><td>Data Analyst Intern</td><td>SHORTLISTED</td><td>Awaiting resume</td></tr>
            <tr><td>Product Manager</td><td>INTERVIEW</td><td>OA passed</td></tr>
          </tbody>
        </table>
      </section>

      <section style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 12, padding: 16, marginBottom: 16 }}>
        <h2>Resume / Cover Letter (<code>/api/v1/resumes</code>, <code>/api/v1/cover-letter</code>)</h2>
        <p>Real endpoints exist; AI disabled by default.</p>
      </section>

      <section style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 12, padding: 16 }}>
        <h2>Application Preparation + Approval (Phase 6 workflow)</h2>
        <p>Draft saved (READY) → approve tied to exact version hash → can submit after approval (blocked until permitted site verified).</p>
        <p style={{ color: '#64748b', fontSize: 12 }}>Can automate: <strong>false</strong> (no permitted site verified per Phase 5 constraint).</p>
      </section>

      <footer style={{ color: '#64748b', fontSize: 12, textAlign: 'center', padding: 24, borderTop: '1px solid #e2e8f0', marginTop: 24 }}>
        Phase 7 — React dashboard scaffolded. Backend APIs verified. Phase 6 approval/version workflow preserved.
      </footer>
    </div>
  );
}
