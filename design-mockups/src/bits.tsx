import { useState } from "react";
import type { Movie } from "./data";
import Icons from "./Icons";

/** Provider logo, desaturated — the chrome stays monochrome, shapes carry recognition. */
export function ProviderDot({ icon, name, size = 18 }: { icon?: string | null; name: string; size?: number }) {
  const [bad, setBad] = useState(false);
  if (icon && !bad) {
    return (
      <img
        className="plogo" src={icon} alt={name} title={name} loading="lazy"
        style={{ width: size, height: size }} onError={() => setBad(true)}
      />
    );
  }
  return (
    <span className="plogo plogo-fallback" title={name} style={{ width: size, height: size }}>
      {name.slice(0, 1)}
    </span>
  );
}

export function OfferChips({ offers, kinds, max = 4 }: {
  offers: Movie["offers"];
  kinds: Movie["offers"][number]["type"][];
  max?: number;
}) {
  const sel = offers.filter(o => kinds.includes(o.type));
  if (sel.length === 0) return null;
  const names = new Map(sel.map(o => [o.provider, o.icon] as const));
  const icons = [...names.entries()];
  return (
    <span className="offers">
      {icons.slice(0, max).map(([name, icon]) => <ProviderDot key={name} icon={icon} name={name} size={16} />)}
      {icons.length > max && <span className="offers-more">+{icons.length - max}</span>}
    </span>
  );
}

/** One rating, quietly: the IMDb mark + number (TMDB as fallback). */
export function Rating({ m }: { m: Movie }) {
  if (m.imdb != null) {
    return <span className="rating"><Icons.ImdbMark />{m.imdb.toFixed(1)}</span>;
  }
  if (m.tmdb != null) {
    return <span className="rating"><Icons.TmdbMark />{m.tmdb.toFixed(1)}</span>;
  }
  return null;
}

/** Detail-page verdict row: IMDb + tomato, monochrome marks, no chips. */
export function Verdict({ m }: { m: Movie }) {
  return (
    <span className="verdict">
      {m.imdb != null && <span className="verdict-item"><Icons.ImdbMark height={12} />{m.imdb.toFixed(1)}</span>}
      {m.tomato != null
        ? <span className="verdict-item"><Icons.TomatoMark />{m.tomato}%</span>
        : m.tmdb != null && <span className="verdict-item"><Icons.TmdbMark height={10} />{m.tmdb.toFixed(1)}</span>}
    </span>
  );
}
