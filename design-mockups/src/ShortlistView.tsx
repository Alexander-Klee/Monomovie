import { useMemo, useState } from "react";
import { shortlistAt, byId, type Movie } from "./data";
import { OfferChips, Rating } from "./bits";
import Icons from "./Icons";

function fmtDate(ts: number) {
  return new Date(ts * 1000).toLocaleDateString("en-GB", { day: "numeric", month: "short" });
}

function Tile({ m, picked, seen, togglePick, toggleWatched, open }: {
  m: Movie; picked: boolean; seen: boolean;
  togglePick: (id: string) => void; toggleWatched: (id: string) => void; open: (id: string) => void;
}) {
  const added = shortlistAt[m.id];
  return (
    <article className={`tile${picked ? " is-picked" : ""}${seen ? " is-seen" : ""}`}>
      <div className="tile-art">
        <button className="tile-open" onClick={() => open(m.id)} aria-label={`Open ${m.title}`}>
          {m.poster
            ? <img src={m.poster} alt="" loading="lazy" />
            : <span className="poster-fallback" aria-hidden="true">{m.title.slice(0, 1)}</span>}
        </button>
        <div className="tile-shade" aria-hidden="true">
          <OfferChips offers={m.offers} kinds={["flatrate", "free"]} />
          {added != null && <span className="tile-added" title="Added to shortlist">{fmtDate(added)}</span>}
        </div>
        <div className="tile-actions">
          <button
            className={`tile-act act-seen${seen ? " is-on" : ""}`}
            onClick={() => toggleWatched(m.id)}
            aria-pressed={seen}
            title={seen ? "Mark unwatched" : "Mark watched"}
          >
            <Icons.Check width={14} height={14} />
          </button>
          <button
            className={`tile-act act-pick${picked ? " is-on" : ""}`}
            onClick={() => togglePick(m.id)}
            aria-pressed={picked}
            title={picked ? "Remove from tonight" : "Pick for tonight"}
          >
            <Icons.Die width={14} height={14} />
          </button>
        </div>
      </div>
      <h3 className="tile-title">
        <button onClick={() => open(m.id)}>{m.title}</button>
      </h3>
      <p className="tile-meta">
        <span>{m.year}{m.runtime ? ` · ${m.runtime} min` : ""}</span>
        <Rating m={m} />
      </p>
    </article>
  );
}

export default function ShortlistView({ picks, watched, togglePick, toggleWatched, open }: {
  picks: Set<string>; watched: Set<string>;
  togglePick: (id: string) => void; toggleWatched: (id: string) => void; open: (id: string) => void;
}) {
  const [hideSeen, setHideSeen] = useState(true);
  const [streamingOnly, setStreamingOnly] = useState(false);
  const sorted = useMemo(
    () =>
      Object.keys(shortlistAt)
        .map(id => byId[id])
        .filter(m => m && !(hideSeen && watched.has(m.id)))
        .filter(m => !streamingOnly || m.offers.some(o => o.type === "flatrate" || o.type === "free"))
        .sort((a, b) => (shortlistAt[b.id] ?? 0) - (shortlistAt[a.id] ?? 0)),
    [watched, hideSeen, streamingOnly],
  );

  return (
    <section className="view">
      <div className="view-head">
        <h1>Shortlist <span className="count">{sorted.length}</span></h1>
        <div className="filters">
          <button
            className={`filter${streamingOnly ? " is-on" : ""}`} aria-pressed={streamingOnly}
            title="Only titles streaming in Germany" onClick={() => setStreamingOnly(v => !v)}
          >
            Streaming
          </button>
          <button
            className={`filter${hideSeen ? " is-on" : ""}`} aria-pressed={hideSeen}
            title="Hide watched titles" onClick={() => setHideSeen(v => !v)}
          >
            Unwatched
          </button>
        </div>
      </div>

      {sorted.length === 0 ? (
        <div className="empty">
          <p>Nothing here.</p>
          <p className="empty-note">Search to add titles, or loosen the filters.</p>
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
