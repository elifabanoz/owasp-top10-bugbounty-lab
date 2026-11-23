import type { FormEvent } from 'react';
import type { Program, Report } from '../types';

type Props = {
  reports: Report[];
  programs: Program[];
  researcher: boolean;
  query: string;
  newTitle: string;
  newDescription: string;
  newSeverity: string;
  onQueryChange: (value: string) => void;
  onSelect: (id: number) => void;
  onTitleChange: (value: string) => void;
  onDescriptionChange: (value: string) => void;
  onSeverityChange: (value: string) => void;
  onCreate: (event: FormEvent<HTMLFormElement>) => void;
};

export default function ReportList(props: Props) {
  return <section className="panel">
    <div className="sectionHead"><h2>Reports</h2><span>{props.reports.length} shown</span></div>
    <input aria-label="Search reports" placeholder="Search report titles" value={props.query}
      onChange={e => props.onQueryChange(e.target.value)} />
    <div className="reportList">{props.reports.map(report => <button className="reportRow" key={report.id}
      onClick={() => props.onSelect(report.id)}>
      <span><strong>#{report.id} {report.title}</strong><small>{report.status}</small></span><em>{report.severity}</em>
    </button>)}</div>
    {props.researcher && props.programs.length > 0 && <form className="newReport" onSubmit={props.onCreate}>
      <h3>New report</h3>
      <p className="programName">{props.programs[0].name}</p>
      <label>Title<input value={props.newTitle} onChange={e => props.onTitleChange(e.target.value)} maxLength={200} required /></label>
      <label>Description<textarea value={props.newDescription} onChange={e => props.onDescriptionChange(e.target.value)} maxLength={10000} required /></label>
      <label>Severity<select value={props.newSeverity} onChange={e => props.onSeverityChange(e.target.value)}>
        <option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option>
      </select></label>
      <button>Submit report</button>
    </form>}
  </section>;
}
