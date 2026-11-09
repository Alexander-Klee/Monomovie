import { useMemo, useState } from "react";
import { shortlistAt, byId, type Movie } from "./data";
import { OfferChips, Verdict } from "./Header";
import Icons from "./Icons";

function fmtDate(ts: number) {
  return new Date(ts * 1000).toLocaleDateString("en-GB", { day: "numeric", month: "short" });
}

function Tile({ m, picked, seen, togglePick, toggleWatched, open }: {
  m: Movie; picked: boolean; seen: boolean;
  togglePick: (id: string) => void; toggleWatched: (id: string) => void; open: (id: string) => void;
}) {
  return (
    <article className={`tile${picked ? " tile-picked" : ""}${seen ? " tile-seen" : ""}`}>
      <button className="tile-art" onClick={() => open(m.id)} aria-label={`Open ${m.title}`}>
        {m.poster
          ? <img src={m.poster} alt="" loading="lazy" />
          : <span className="poster-fallback" aria-hidden="true">{m.title.slice(0, 1)}</span>}
        {seen && (
          <span className="tile-seen-badge" title="Watched">
            <Icons.Eye width={13} height={13} />
          </span>
        )}
      </button>
      <button
        className={`tile-pick${picked ? " is-on" : ""}`}
        onClick={() => togglePick(m.id)}
        aria-pressed={picked}
        aria-label={picked ? `Remove ${m.title} from tonight's picks` : `Pick ${m.title} for tonight`}
        title={picked ? "Unpick" : "Pick for tonight"}
      >
        {picked ? <Icons.BookmarkFilled width={15} height={15} /> : <Icons.BookmarkPlus width={15} height={15} />}
      </button>
      <div className="tile-body">
        <h3 className="tile-title">{m.title}</h3>
        <p className="tile-meta">
          {m.year}{m.runtime ? ` · ${m.runtime} min` : ""}
          <span className="spacer" />
          <span className="tile-added" title="Added to shortlist">+{fmtDate(shortlistAt[m.id] ?? 0)}</span>
        </p>
        <div className="tile-bottom">
          <OfferChips offers={m.offers} kinds={["flatrate", "free"]} />
          <span className="tile-imdb">{m.imdb != null ? m.imdb.toFixed(1) : "—"}</span>
        </div>
      </div>
      <div className="tile-more" role="presentation">
        <div className="tile-more-inner">
          <p className="tile-desc">{m.desc}</p>
          <p className="tile-genres">{m.genres.join(", ")}</p>
          <div className="tile-more-row">
            <Verdict m={m} />
            <span className="tile-genres-2">{m.cert != null && <span>FSK {m.cert}</span>}{m.rank != null && <span>#{m.rank}</span>}</span>
          </div>
          <div className="tile-actions">
            <button className="chip-btn" onClick={e => { e.stopPropagation(); toggleWatched(m.id); }}>
              {seen ? <>Unwatch <Icons.EyeOff width={13} height={13} /></> : <>Watched <Icons.Eye width={13} height={13} /></>}
            </button>
            <button className="chip-btn" onClick={e => { e.stopPropagation(); togglePick(m.id); }}>
              {picked ? "Unpick" : "Pick for tonight"}
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}

export default function ShortlistView({ picks, watched, togglePick, toggleWatched, open }: {
  picks: Set<string>; watched: Set<string>;
  togglePick: (id: string) => void; toggleWatched: (id: string) => void; open: (id: string) => void;
}) {
  const [hideSeen, setHideSeen] = useState(true);
  const [streamingOnly, setStreamingOnly] = useState(false);
  const list = useMemo(
    () =>
      Object.keys(shortlistAt)
        .map(id => byId[id])
        .filter(m => m && !(hideSeen && watched.has(m.id)))
        .filter(m => !streamingOnly || m.offers.some(o => o.type === "flatrate" || o.type === "free")),
    [watched, hideSeen, streamingOnly],
  );
  const sorted = useMemo(
    () => [...list].sort((a, b) => (shortlistAt[b.id] ?? 0) - (shortlistAt[a.id] ?? 0)),
    [list],
  );
  const pickedCount = [...picks].filter(id => !watched.has(id)).length;

  return (
    <section className="view">
      <div className="view-head">
        <div>
          <h1>Shortlist</h1>
          <p className="view-sub">
            {sorted.length} titles waiting · {pickedCount} picked for tonight
          </p>
        </div>
        <div className="filters">
          <button className={`filter${streamingOnly ? " on" : ""}`} aria-pressed={streamingOnly} onClick={() => setStreamingOnly(v => !v)}>
            Streaming in DE
          </button>
          <button className={`filter${hideSeen ? " on" : ""}`} aria-pressed={hideSeen} onClick={() => setHideSeen(v => !v)}>
            Hide watched
          </button>
        </div>
      </div>

      {sorted.length === 0 ? (
        <div className="empty">
          <h2>Nothing left on the shelf.</h2>
          <p>
            Your shortlist is cleared out. Everything you saved has been watched —{" "}
            <a href="#/">press /</a> to search for the next one.
          </p>
        </div>
      ) : (
        <div className="grid">
          {sorted.map(m => (
            <Tile
              key={m.id}
              m={m}
              picked={picks.has(m.id)}
              seen={watched.has(m.id)}
              togglePick={togglePick}
              toggleWatched={toggleWatched}
              open={open}
            />
          ))}
        </div>
      )}
    </section>
  );
}
