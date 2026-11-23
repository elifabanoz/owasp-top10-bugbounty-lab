import type { FormEvent } from 'react';
import type { Comment, Profile, Report } from '../types';

type Props = {
  selected: Report | null;
  comments: Comment[];
  comment: string;
  profile: Profile | null;
  onCommentChange: (value: string) => void;
  onAddComment: (event: FormEvent<HTMLFormElement>) => void;
};

export default function ReportDetail({ selected, comments, comment, profile, onCommentChange, onAddComment }: Props) {
  return <section className="panel detail">
    {selected ? <>
      <div className="sectionHead"><h2>{selected.title}</h2><span>#{selected.id}</span></div>
      <p className="eyebrow">{selected.severity} · {selected.status}</p>
      {profile === 'vulnerable'
        ? <div className="bodyText" dangerouslySetInnerHTML={{ __html: selected.description }} />
        : <div className="bodyText">{selected.description}</div>}
      <h3>Comments</h3>
      {comments.map(item => <div className="comment" key={item.id}>
        <small>Author #{item.authorId}</small>
        {profile === 'vulnerable'
          ? <div dangerouslySetInnerHTML={{ __html: item.body }} />
          : <div>{item.body}</div>}
      </div>)}
      <form onSubmit={onAddComment} className="commentForm">
        <label>Add comment<textarea value={comment} onChange={e => onCommentChange(e.target.value)} required /></label>
        <button>Post comment</button>
      </form>
    </> : <p className="empty">Select a report to see its details.</p>}
  </section>;
}
