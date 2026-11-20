import { useEffect, useRef, useState } from "react";
import { movies } from "./data";
import { Rating } from "./bits";
import Icons from "./Icons";
import type { Route } from "./App";

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
            <span className="search-sub">{m.year}{m.type === "show" ? " · Series" : ""}</span>
          </span>
          <Rating m={m} />
        </button>
      ))}
      {results.length === 0 && (
        <div className="search-empty">
          No match in the cache.
          <span className="search-empty-note">The app searches JustWatch live from here.</span>
        </div>
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
    { label: "Shortlist", target: "/", active: route.name === "shortlist" || route.name === "movie" },
    { label: "Roulette", target: "/roulette", active: route.name === "roulette" },
    { label: "Watched", target: "/watched", active: route.name === "watched" },
  ];

  return (
    <header className="header">
      <a className="wordmark" href="#/" onClick={() => setFocused(false)}>
        <Icons.Logo width={21} height={21} />
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
        <Icons.Search width={14} height={14} />
        <input
          ref={searchRef}
          type="search"
          placeholder="Search"
          value={q}
          onChange={e => setQ(e.target.value)}
          onFocus={() => setFocused(true)}
          aria-label="Search titles"
        />
        {q ? (
          <button className="search-clear" aria-label="Clear search" onClick={() => { setQ(""); searchRef.current?.focus(); }}>
            <Icons.X width={13} height={13} />
          </button>
        ) : (
          <kbd className="search-kbd" aria-hidden="true">/</kbd>
        )}
        {focused && <Results q={q} open={open} close={() => { setFocused(false); (document.activeElement as HTMLElement)?.blur(); }} />}
      </div>
    </header>
  );
}
