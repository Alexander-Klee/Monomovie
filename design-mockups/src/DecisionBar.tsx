import { type Movie } from "./data";
import Icons from "./Icons";

export default function DecisionBar({ picks, togglePick }: {
  picks: Movie[];
  togglePick: (id: string) => void;
}) {
  if (picks.length === 0) return null;
  return (
    <div className="dbar" role="toolbar" aria-label="Tonight's picks">
      <div className="dbar-thumbs">
        {picks.slice(0, 4).map(m => (
          <span key={m.id} className="dbar-thumb" style={{ ["--swatch" as string]: m.wheel }}>
            {m.poster && <img src={m.poster} alt={m.title} loading="lazy" />}
            <button
              className="dbar-thumb-x"
              aria-label={`Remove ${m.title} from tonight's picks`}
              onClick={() => togglePick(m.id)}
            >
              <Icons.X width={10} height={10} />
            </button>
          </span>
        ))}
        {picks.length > 4 && <span className="dbar-more">+{picks.length - 4}</span>}
      </div>
      <p className="dbar-copy">
        <strong>{picks.length} picked</strong>
        {picks.length < 2 && <span className="dbar-hint"> — add more to spin</span>}
      </p>
      <div className="dbar-actions">
        <a className={`btn btn-primary${picks.length < 2 ? " is-disabled" : ""}`} href={picks.length >= 2 ? "#/roulette" : undefined} aria-disabled={picks.length < 2}>
          Decide tonight <Icons.Ticket width={15} height={15} />
        </a>
      </div>
    </div>
  );
}

export { };
