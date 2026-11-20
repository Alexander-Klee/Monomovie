import { useState } from "react";
import { type Movie, type Offer } from "./data";
import { Verdict, ProviderDot } from "./bits";
import Icons from "./Icons";

const GROUPS: { key: Offer["type"]; title: string }[] = [
  { key: "flatrate", title: "Stream" },
  { key: "free", title: "Free" },
  { key: "rent", title: "Rent" },
  { key: "buy", title: "Buy" },
];

const OTHER_COUNTRIES = [
  { cc: "US", note: "Netflix" },
  { cc: "AT", note: "Prime Video, Sky" },
  { cc: "CH", note: "Disney+" },
  { cc: "FR", note: "—" },
];

export default function MovieDetail({ movie, picks, watched, togglePick, toggleWatched, nav }: {
  movie: Movie | undefined;
  picks: Set<string>; watched: Set<string>;
  togglePick: (id: string) => void; toggleWatched: (id: string) => void;
  nav: (r: string) => void;
}) {
  const [showAbroad, setShowAbroad] = useState(false);
  if (!movie) {
    return (
      <section className="view">
        <div className="empty"><p>Not in the cache.</p><p className="empty-note">Search for it first.</p></div>
      </section>
    );
  }
  const picked = picks.has(movie.id);
  const seen = watched.has(movie.id);
  const grouped = GROUPS.map(g => ({ ...g, offers: movie.offers.filter(o => o.type === g.key) })).filter(g => g.offers.length > 0);

  return (
    <section className="view detail">
      <button className="back" onClick={() => nav("/")} aria-label="Back" title="Back">
        <Icons.ArrowLeft />
      </button>

      <div className="detail-hero">
        {movie.posterFull
          ? <img className="detail-poster" src={movie.posterFull} alt="" />
          : <span className="detail-poster poster-fallback" aria-hidden="true">{movie.title.slice(0, 1)}</span>}
        <div className="detail-head">
          <h1 className="detail-title">{movie.title}</h1>
          <p className="detail-meta">
            {movie.type === "show" ? `Series · ${movie.year}` : movie.year}
            {movie.runtime ? ` · ${movie.runtime} min` : ""}
            {movie.cert ? ` · FSK ${movie.cert}` : ""}
            {movie.genres.length > 0 ? ` · ${movie.genres.join(", ")}` : ""}
          </p>
          <Verdict m={movie} />
          <p className="detail-desc">{movie.desc}</p>
          <div className="detail-actions">
            <button
              className={`btn${picked ? " is-on" : ""}`} onClick={() => togglePick(movie.id)}
              aria-pressed={picked} title={picked ? "Remove from tonight's draw" : "Add to tonight's draw"}
            >
              <Icons.Die /> Tonight
            </button>
            <button
              className={`btn${seen ? " is-on" : ""}`} onClick={() => toggleWatched(movie.id)}
              aria-pressed={seen} title={seen ? "Mark unwatched" : "Mark watched"}
            >
              <Icons.Check /> Watched
            </button>
          </div>
        </div>
      </div>

      <div className="detail-body">
        <h2>Where to watch in Germany</h2>
        {grouped.length === 0 ? (
          <p className="detail-none">No current offers.</p>
        ) : (
          <div className="offer-groups">
            {grouped.map(g => (
              <div key={g.key} className="offer-group">
                <p className="offer-group-title">{g.title}</p>
                <ul className="offer-list">
                  {g.offers.map(o => (
                    <li key={o.provider}>
                      <a className="offer-row" href="#" onClick={e => e.preventDefault()}>
                        <ProviderDot icon={o.icon ?? null} name={o.provider} size={20} />
                        <span className="offer-name">{o.provider}</span>
                        {o.price && <span className="offer-price">{o.price}</span>}
                        <Icons.ArrowUpRight className="offer-go" width={12} height={12} />
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}

        <button className={`disclose${showAbroad ? " is-open" : ""}`} onClick={() => setShowAbroad(v => !v)} aria-expanded={showAbroad}>
          Other countries <Icons.ChevronDown width={13} height={13} />
        </button>
        {showAbroad && (
          <ul className="abroad-list">
            {OTHER_COUNTRIES.map(c => (
              <li key={c.cc}>
                <span className="abroad-cc">{c.cc}</span>
                <span className="abroad-note">{c.note}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
