import { useState } from "react";
import { type Movie, type Offer } from "./data";
import { Verdict, ProviderDot } from "./Header";
import Icons from "./Icons";

const GROUPS: { key: Offer["type"]; title: string; note: string }[] = [
  { key: "flatrate", title: "Included in subscription", note: "free with these plans" },
  { key: "free", title: "Free with ads", note: "no account cost" },
  { key: "rent", title: "Rent", note: "one-time" },
  { key: "buy", title: "Buy", note: "digital or disc" },
];

const OTHER_COUNTRIES = [
  { cc: "US", note: "Netflix (flatrate)", flag: "🇺🇸" },
  { cc: "AT", note: "Prime Video, Sky (flatrate)", flag: "🇦🇹" },
  { cc: "CH", note: "Disney+ (flatrate)", flag: "🇨🇭" },
  { cc: "FR", note: "No offers found", flag: "🇫🇷" },
];

export default function MovieDetail({ movie, picks, watched, togglePick, toggleWatched, nav }: {
  movie: Movie | undefined;
  picks: Set<string>; watched: Set<string>;
  togglePick: (id: string) => void;
  toggleWatched: (id: string) => void;
  nav: (r: string) => void;
}) {
  const [showAbroad, setShowAbroad] = useState(false);
  if (!movie) {
    return (
      <section className="view">
        <div className="empty"><h2>Not in the cache.</h2><p>This entry hasn’t been fetched yet — search for it first.</p></div>
      </section>
    );
  }
  const picked = picks.has(movie.id);
  const seen = watched.has(movie.id);
  const de = movie.offers;
  const grouped = GROUPS.map(g => ({ ...g, offers: de.filter(o => o.type === g.key) })).filter(g => g.offers.length > 0);

  return (
    <section className="view detail">
      <div
        className="detail-hero"
        style={movie.backdrop ? { ["--backdrop" as string]: `url(${movie.backdrop})` } : undefined}
      >
        <button className="crumb" onClick={() => nav("/")}>
          <Icons.Undo width={14} height={14} /> Back to shortlist
        </button>
        <div className="detail-hero-inner">
          {movie.posterFull
            ? <img className="detail-poster" src={movie.posterFull} alt={`${movie.title} poster`} />
            : <span className="detail-poster poster-fallback" aria-hidden="true">{movie.title.slice(0, 1)}</span>}
          <div className="detail-head">
            <p className="detail-kind">{movie.rank != null ? <>Trending in Germany — #{movie.rank}</> : movie.type === "show" ? "Series" : null}</p>
            <h1 className="detail-title">{movie.title}</h1>
            <p className="detail-meta">
              {movie.type === "show" ? "Series" : "Movie"} · {movie.year} · {movie.runtime} min{movie.cert ? ` · FSK ${movie.cert}` : ""}
              {movie.genres.length > 0 && <> — {movie.genres.join(", ")}</>}
            </p>
            <Verdict m={movie} />
            <p className="detail-desc">{movie.desc}</p>
            <div className="detail-actions">
              <button className={`btn btn-primary${picked ? " is-on" : ""}`} onClick={() => togglePick(movie.id)}>
                <Icons.BookmarkFilled width={15} height={15} />
                {picked ? "Picked for tonight" : "Pick for tonight"}
              </button>
              <button className="btn" onClick={() => toggleWatched(movie.id)}>
                {seen ? <>Unwatch <Icons.EyeOff width={15} height={15} /></> : <>Mark as watched <Icons.Eye width={15} height={15} /></>}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="detail-body">
        <h2 className="detail-section-title">Where to watch in Germany</h2>
        {grouped.length === 0 ? (
          <p className="detail-none">No current offers in Germany right now — find what’s out there:</p>
        ) : (
          <div className="offer-groups">
            {grouped.map(g => (
              <div key={g.key} className="offer-group">
                <p className="offer-group-title">{g.title}</p>
                <ul className="offer-list">
                  {g.offers.map(o => (
                    <li key={o.provider}>
                      <a className="offer-row" href="#" onClick={e => e.preventDefault()}>
                        <ProviderDot icon={o.icon ?? null} name={o.provider} />
                        <span className="offer-name">{o.provider}</span>
                        <span className="offer-price">{o.price ?? "included"}</span>
                        <Icons.ArrowUp className="offer-go" width={12} height={12} />
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}

        <h2 className="detail-section-title">Abroad</h2>
        {showAbroad ? (
          <ul className="abroad-list">
            {OTHER_COUNTRIES.map(c => (
              <li key={c.cc}>
                <span className="abroad-flag" aria-hidden="true">{c.flag}</span>
                <span className="abroad-cc">{c.cc}</span>
                <span>{c.note}</span>
              </li>
            ))}
          </ul>
        ) : (
          <button className="linkish" onClick={() => setShowAbroad(true)}>
            Check other countries (24 more)
          </button>
        )}
      </div>
    </section>
  );
}
