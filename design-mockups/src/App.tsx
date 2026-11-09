import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { byId } from "./data";
import Header from "./Header";
import DecisionBar from "./DecisionBar";
import ShortlistView from "./ShortlistView";
import MovieDetail from "./MovieDetail";
import RouletteView from "./RouletteView";
import WatchedView from "./WatchedView";

export type Route =
  | { name: "shortlist" }
  | { name: "movie"; id: string }
  | { name: "roulette" }
  | { name: "watched" };

function parseHash(): Route {
  const h = window.location.hash.replace(/^#\/?/, "");
  if (h.startsWith("movie/")) return { name: "movie", id: h.slice("movie/".length) };
  if (h === "roulette") return { name: "roulette" };
  if (h === "watched") return { name: "watched" };
  return { name: "shortlist" };
}

function useRoute(): [Route, (r: string) => void] {
  const [route, setRoute] = useState<Route>(parseHash);
  useEffect(() => {
    const onChange = () => setRoute(parseHash());
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);
  const nav = useCallback((r: string) => {
    window.location.hash = r;
  }, []);
  return [route, nav];
}

export default function App() {
  const [route, nav] = useRoute();
  // picks = "picked for tonight" (subset of shortlist)
  const [picks, setPicks] = useState<Set<string>>(
    () => new Set(["tm10", "tm155580", "tm122851"]),
  );
  const [weights, setWeights] = useState<Record<string, number>>({});
  const [watched, setWatched] = useState<Set<string>>(() => new Set(["tm56184", "tm44313", "tm362", "tm138000", "tm128715"]));

  const togglePick = (id: string) =>
    setPicks(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  const toggleWatched = (id: string) =>
    setWatched(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const weightOf = (id: string) => weights[id] ?? 1;
  const setWeight = (id: string, w: number) =>
    setWeights(prev => ({ ...prev, [id]: Math.max(0, w) }));

  const pickedMovies = useMemo(() => [...picks].map(id => byId[id]).filter(Boolean), [picks]);

  // keyboard: "/" focuses search
  const searchRef = useRef<HTMLInputElement | null>(null);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      if (e.key === "/" && tag !== "INPUT" && tag !== "TEXTAREA") {
        e.preventDefault();
        searchRef.current?.focus();
      }
      if (e.key === "Escape") (document.activeElement as HTMLElement)?.blur();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const open = (id: string) => nav(`/movie/${id}`);

  return (
    <div className="app">
      <Header searchRef={searchRef} route={route} open={open} />
      <main className="app-main">
        {route.name === "shortlist" && (
          <ShortlistView
            picks={picks}
            watched={watched}
            togglePick={togglePick}
            toggleWatched={toggleWatched}
            open={open}
          />
        )}
        {route.name === "movie" && (
          <MovieDetail movie={byId[route.id]} picks={picks} watched={watched} togglePick={togglePick} toggleWatched={toggleWatched} nav={nav} />
        )}
        {route.name === "roulette" && (
          <RouletteView pickedMovies={pickedMovies} weightOf={weightOf} setWeight={setWeight} open={open} />
        )}
        {route.name === "watched" && (
          <WatchedView watched={watched} toggleWatched={toggleWatched} open={open} />
        )}
      </main>
      {route.name !== "roulette" && route.name !== "movie" && pickedMovies.length > 0 && (
        <DecisionBar picks={pickedMovies} togglePick={togglePick} />
      )}
    </div>
  );
}
