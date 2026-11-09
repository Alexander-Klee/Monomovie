import { useEffect, useRef, useState } from "react";
import { movies, type Movie } from "./data";
import Icons from "./Icons";
import type { Route } from "./App";

/** tiny brand/monochrome provider glyph — JustWatch CDN icons vary wildly, fallback keeps it quiet */
export function ProviderDot({ icon, name }: { icon?: string | null; name: string }) {
  const [bad, setBad] = useState(false);
  if (icon && !bad) {
    return <img className="plogo" src={icon} alt="" loading="lazy" onError={() => setBad(true)} title={name} />;
  }
  return (
    <span className="plogo plogo-fallback" title={name} aria-hidden="true">
      {name.slice(0, 1)}
    </span>
  );
}

export function OfferChips({ offers, kinds }: { offers: Movie["offers"]; kinds: Movie["offers"][number]["type"][] }) {
  const sel = offers.filter(o => kinds.includes(o.type));
  if (sel.length === 0) return <span className="offers-none">not streaming (DE)</span>;
  const names = new Map(sel.map(o => [o.provider, o.icon] as const));
  const icons = [...names.entries()];
  return (
    <span className="offers">
      {icons.slice(0, 5).map(([name, icon]) => <ProviderDot key={name} icon={icon} name={name} />)}
      {icons.length > 5 && <span className="offers-more">+{icons.length - 5}</span>}
    </span>
  );
}

export function Verdict({ m }: { m: Movie }) {
  const parts: (string | null)[] = [];
  if (m.imdb != null) parts.push(m.imdb.toFixed(1));
  if (m.tomato != null) parts.push(`${m.tomato}%`);
  else if (m.tmdb != null) parts.push(m.tmdb.toFixed(1));
  return (
    <span className={`verdict`}>
      {m.imdb != null && <span className="verdict-imdb">IMDb {m.imdb.toFixed(1)}</span>}
      {m.tomato != null && <span className="verdict-tomato">{m.tomato}%&nbsp;fresh</span>}
      {m.tomato == null && m.tmdb != null && <span>TM {m.tmdb.toFixed(1)}</span>}
      <span className="visually-hidden"> of {parts.join(" / ")} average</span>
    </span>
  );
}

function Results({ q, open, close }: { q: string; open: (id: string) => void; close: () => void }) {
  const ql = q.trim().toLowerCase();
  if (!ql) return null;
  const results = movies
    .filter(m => m.title.toLowerCase().includes(ql))
    .sort((a, b) => (b.imdb ?? 0) - (a.imdb ?? 0))
    .slice(0, 7);
  return (
    <div className="search-results" role="listbox" aria-label="Search results">
      {results.map(m => (
        <button key={m.id} className="search-row" onClick={() => { open(m.id); close(); }}>
          {m.poster
            ? <img src={m.poster} alt="" className="search-poster" loading="lazy" />
            : <span className="search-poster search-poster-empty" aria-hidden="true" />}
          <span className="search-meta">
            <span className="search-title">{m.title}</span>
            <span className="search-sub">
              {m.type === "show" ? "Series" : "Movie"}{m.year ? ` · ${m.year}` : ""}
              {m.offers.some(o => o.type === "flatrate") && <span className="search-streaming"> · on streaming now</span>}
            </span>
          </span>
          <span className="verdict verdict-compact verdict-imdb">{m.imdb != null ? m.imdb.toFixed(1) : ""}</span>
        </button>
      ))}
      {results.length === 0 && (
        <div className="search-empty">No match in the cache.<br /><span className="muted-note">In the real app the search continues live on JustWatch here.</span></div>
      )}
    </div>
  );
}

export default function Header({ searchRef, route, open }: {
  searchRef: React.RefObject<HTMLInputElement | null>;
  route: Route;
  open: (id: string) => void;
}) {
  const [q, setQ] = useState("");
  const [focused, setFocused] = useState(false);
  const wrap = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onDown = (e: MouseEvent) => {
      if (!wrap.current?.contains(e.target as Node)) setFocused(false);
    };
    window.addEventListener("mousedown", onDown);
    return () => window.removeEventListener("mousedown", onDown);
  }, []);

  const tabs: { label: string; target: string; active: boolean }[] = [
    { label: "Shortlist", target: "/", active: route.name === "shortlist" },
    { label: "Roulette", target: "/roulette", active: route.name === "roulette" },
    { label: "Watched", target: "/watched", active: route.name === "watched" },
  ];

  return (
    <header className="header">
      <a className="wordmark" href="#/" onClick={() => setFocused(false)}>
        <svg width="22" height="22" viewBox="0 0 32 32" aria-hidden="true">
          <path d="M9 4h14v24l-7-5-7 5z" fill="#F69E45" />
        </svg>
        Monomovie
      </a>
      <nav className="tabs" aria-label="Sections">
        {tabs.map(t => (
          <a key={t.target} className={`tab${t.active ? " tab-active" : ""}`} href={`#${t.target}`}>
            {t.label}
          </a>
        ))}
      </nav>
      <div className="search" ref={wrap}>
        <Icons.Search />
        <input
          ref={searchRef}
          type="search"
          placeholder="Search a title…"
          value={q}
          onChange={e => setQ(e.target.value)}
          onFocus={() => setFocused(true)}
          aria-label="Search cached and JustWatch titles"
        />
        {q && (
          <button className="search-clear" aria-label="Clear search" onClick={() => { setQ(""); searchRef.current?.focus(); }}>
            <Icons.X width={14} height={14} />
          </button>
        )}
        {focused && <Results q={q} open={open} close={() => { setFocused(false); (document.activeElement as HTMLElement)?.blur(); }} />}
      </div>
      <span className="header-hint" aria-hidden="true">press / to search</span>
    </header>
  );
}
