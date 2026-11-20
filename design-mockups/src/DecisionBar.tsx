import { type Movie } from "./data";
import Icons from "./Icons";

/** Persistent "tonight" tray: the picked posters themselves are the count. */
export default function DecisionBar({ picks, togglePick }: {
  picks: Movie[];
  togglePick: (id: string) => void;
}) {
  if (picks.length === 0) return null;
  const ready = picks.length >= 2;
  return (
    <div className="dock" role="toolbar" aria-label="Tonight's picks">
      <div className="dock-thumbs">
        {picks.slice(0, 5).map(m => (
          <span key={m.id} className="dock-thumb">
            {m.poster
              ? <img src={m.poster} alt={m.title} loading="lazy" />
              : <span className="dock-thumb-empty" aria-hidden="true" />}
            <button
              className="dock-x"
              aria-label={`Remove ${m.title} from tonight`}
              title={`Remove ${m.title}`}
              onClick={() => togglePick(m.id)}
            >
              <Icons.X width={9} height={9} />
            </button>
          </span>
        ))}
        {picks.length > 5 && <span className="dock-more">+{picks.length - 5}</span>}
      </div>
      <a
        className={`btn btn-solid dock-go${ready ? "" : " is-disabled"}`}
        href={ready ? "#/roulette" : undefined}
        aria-disabled={!ready}
        title={ready ? "Set the odds" : "Pick at least two films"}
      >
        Decide <Icons.Die width={14} height={14} />
      </a>
    </div>
  );
}
