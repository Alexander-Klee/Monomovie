import { useMemo } from "react";
import { byId, watched as watchedDates } from "./data";
import Icons from "./Icons";

interface Props {
  watched: Set<string>;
  toggleWatched: (id: string) => void;
  open: (id: string) => void;
}

export default function WatchedView({ watched, toggleWatched, open }: Props) {
  const groups = useMemo(() => {
    const by = new Map<string, { id: string; date: Date }[]>();
    for (const id of [...watched].sort()) {
      const ds = watchedDates[id];
      if (!ds) continue;
      const d = new Date(ds + "T20:00:00");
      const key = d.toLocaleDateString("en-GB", { month: "long", year: "numeric" });
      if (!by.has(key)) by.set(key, []);
      by.get(key)!.push({ id, date: d });
    }
    return [...by.entries()].sort(
      (a, b) => new Date(b[1][0].date).getTime() - new Date(a[1][0].date).getTime(),
    );
  }, [watched]);

  return (
    <section className="view">
      <div className="view-head">
        <div>
          <h1>Watched</h1>
          <p className="view-sub">What got watched, newest first ({watched.size} all time)</p>
        </div>
      </div>
      {groups.length === 0 ? (
        <div className="empty">
          <h2>Nothing logged yet.</h2>
          <p>Tap the eye on any shortlist tile once you have seen it, and it lands here.</p>
        </div>
      ) : (
        groups.map(([month, items]) => (
          <div key={month} className="month">
            <div className="month-head">
              <h2 className="month-name">{month}</h2>
              <span className="month-count">{items.length}</span>
            </div>
            <ul className="ledger">
              {items
                .sort((a, b) => b.date.getTime() - a.date.getTime())
                .map(({ id, date }) => {
                  const m = byId[id];
                  if (!m) return null;
                  return (
                    <li key={id} className="ledger-row">
                      <span className="ledger-date" aria-hidden="true">
                        {date.toLocaleDateString("en-GB", { weekday: "short", day: "2-digit" })}
                                              </span>
                      {m.poster ? (
                        <img className="ledger-poster" src={m.poster} alt="" loading="lazy" />
                      ) : (
                        <span className="ledger-poster ledger-poster-empty" aria-hidden="true" />
                      )}
                      <div className="ledger-meta">
                        <button className="ledger-title" onClick={() => open(id)}>{m.title}</button>
                        <p className="ledger-sub">
                          {m.type === "show" ? "Series" : "Movie"} · {m.year} · {m.runtime} min
                        </p>
                      </div>
                      <button className="icon-btn" onClick={() => toggleWatched(id)} aria-label={`Forgot ${m.title} — remove from history`}>
                        <Icons.Undo width={14} height={14} />
                      </button>
                    </li>
                  );
                })}
            </ul>
          </div>
        ))
      )}
    </section>
  );
}
