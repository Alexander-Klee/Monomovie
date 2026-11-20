import { useMemo, useState } from "react";
import { type Movie } from "./data";
import { mockQr } from "./qr";
import Icons from "./Icons";

/** Monochrome segment ramp — lightness distinguishes entries, posters carry the colour elsewhere. */
const SHADES = ["#e8e5de", "#b4b1a9", "#817f78", "#55544f", "#373633"];
const shade = (i: number) => SHADES[i % SHADES.length];

function polar(cx: number, cy: number, r: number, deg: number) {
  const rad = ((deg - 90) * Math.PI) / 180;
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) };
}
/** filled annular sector from a0 to a1 (degrees, clockwise from 12 o'clock) */
function sectorPath(cx: number, cy: number, r0: number, r1: number, a0: number, a1: number) {
  const p1 = polar(cx, cy, r1, a0);
  const p2 = polar(cx, cy, r1, a1);
  const p3 = polar(cx, cy, r0, a1);
  const p4 = polar(cx, cy, r0, a0);
  const large = a1 - a0 > 180 ? 1 : 0;
  return [
    `M ${p1.x.toFixed(2)} ${p1.y.toFixed(2)}`,
    `A ${r1} ${r1} 0 ${large} 1 ${p2.x.toFixed(2)} ${p2.y.toFixed(2)}`,
    `L ${p3.x.toFixed(2)} ${p3.y.toFixed(2)}`,
    `A ${r0} ${r0} 0 ${large} 0 ${p4.x.toFixed(2)} ${p4.y.toFixed(2)}`,
    "Z",
  ].join(" ");
}

function Wheel({ picks, weightOf }: { picks: Movie[]; weightOf: (id: string) => number }) {
  const on = picks.map((m, i) => ({ m, i, w: weightOf(m.id) })).filter(s => s.w > 0);
  const total = on.reduce((sum, s) => sum + s.w, 0) || 1;
  let acc = 0;
  const segments = on.map(s => {
    const span = (s.w / total) * 360;
    const seg = { ...s, start: acc, end: acc + span };
    acc += span;
    return seg;
  });
  return (
    <svg className="wheel" viewBox="0 0 240 240" role="img" aria-label="Odds preview">
      {segments.map(({ m, i, start, end }) => {
        const pad = segments.length > 1 ? 1.25 : 0;
        return <path key={m.id} d={sectorPath(120, 120, 76, 110, start + pad, end - pad)} fill={shade(i)} />;
      })}
      {segments.length === 0 && (
        <circle cx="120" cy="120" r="93" fill="none" stroke="var(--line)" strokeWidth="34" />
      )}
    </svg>
  );
}

function Stepper({ w, onChange }: { w: number; onChange: (n: number) => void }) {
  return (
    <span className="stepper">
      <button aria-label="Fewer chances" onClick={() => onChange(w - 1)} disabled={w === 0}>
        <Icons.Minus width={12} height={12} />
      </button>
      <output className={w === 0 ? "stepper-zero" : ""}>{w}</output>
      <button aria-label="More chances" onClick={() => onChange(w + 1)}>
        <Icons.Plus width={12} height={12} />
      </button>
    </span>
  );
}

export default function RouletteView({ pickedMovies, weightOf, setWeight, open }: {
  pickedMovies: Movie[];
  weightOf: (id: string) => number;
  setWeight: (id: string, w: number) => void;
  open: (id: string) => void;
}) {
  const [shared, setShared] = useState(false);
  const totalWeight = pickedMovies.reduce((sum, m) => sum + weightOf(m.id), 0);
  const odds = useMemo(
    () => Object.fromEntries(pickedMovies.map(m => [
      m.id,
      weightOf(m.id) === 0 ? "—" : `${Math.round((weightOf(m.id) / (totalWeight || 1)) * 100)}%`,
    ])),
    [pickedMovies, weightOf, totalWeight],
  );

  if (pickedMovies.length === 0) {
    return (
      <section className="view">
        <div className="empty">
          <p>Nothing to draw from.</p>
          <p className="empty-note">Pick films on the <a href="#/">shortlist</a> first.</p>
        </div>
      </section>
    );
  }

  return (
    <section className="view roulette">
      <div className="view-head">
        <h1>Roulette</h1>
      </div>
      <div className="roulette-grid">
        <ul className="roulette-list" aria-label="Films in the draw">
          {pickedMovies.map((m, i) => {
            const w = weightOf(m.id);
            return (
              <li key={m.id} className={`roulette-row${w === 0 ? " is-out" : ""}`}>
                <span
                  className="roulette-swatch" aria-hidden="true"
                  style={w > 0 ? { background: shade(i) } : undefined}
                />
                <button className="roulette-title" onClick={() => open(m.id)}>{m.title}</button>
                <span className="roulette-odds">{odds[m.id]}</span>
                <Stepper w={w} onChange={n => setWeight(m.id, n)} />
              </li>
            );
          })}
        </ul>
        <div className="roulette-stage">
          <Wheel picks={pickedMovies} weightOf={weightOf} />
          {totalWeight > 0 ? (
            <a className="btn btn-solid" href="https://providence.jfronny.dev" target="_blank" rel="noreferrer">
              Open the wheel <Icons.ArrowUpRight width={13} height={13} />
            </a>
          ) : (
            <p className="stage-note">Give at least one film a weight.</p>
          )}
          <p className="stage-note">Spins on Providence.</p>
        </div>
      </div>

      <div className="share-panel">
        {!shared ? (
          <button className="btn" onClick={() => setShared(true)}>
            <Icons.Qr /> Share
          </button>
        ) : (
          <div className="share-open" role="status">
            <div className="share-qr" dangerouslySetInnerHTML={{ __html: mockQr }} />
            <div className="share-meta">
              <p className="share-code">TONIGHT-77</p>
              <p className="share-url">mmv.amklee.de/roulette</p>
              <p className="share-note">2 joined</p>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
