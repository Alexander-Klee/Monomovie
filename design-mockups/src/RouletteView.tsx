import { useMemo, useState } from "react";
import { type Movie } from "./data";
import { mockQr } from "./qr";
import Icons from "./Icons";

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

function Wheel({ picks, weights }: {
  picks: Movie[]; weights: Record<string, number>;
}) {
  const on = picks.filter(m => (weights[m.id] ?? 1) > 0);
  const total = on.reduce((sum, m) => sum + (weights[m.id] ?? 1), 0) || 1;
  let acc = 0;
  const segments = on.map(m => {
    const span = ((weights[m.id] ?? 1) / total) * 360;
    const seg = { m, start: acc, end: acc + span };
    acc += span;
    return seg;
  });
  return (
    <svg className="wheel" viewBox="0 0 240 240" role="img" aria-label="Decision wheel preview">
      <defs>
        <filter id="wheel-shadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="3" stdDeviation="6" floodOpacity="0.45" />
        </filter>
      </defs>
      <g>
        {segments.map(({ m, start, end }) => {
          const pad = segments.length > 1 ? 1.5 : 0;
          return (
            <path
              key={m.id}
              d={sectorPath(120, 120, 74, 108, start + pad, end - pad)}
              fill={m.wheel}
            />
          );
        })}
      </g>
      <circle cx="120" cy="120" r="64" fill="rgba(21,19,27,.92)" filter="url(#wheel-shadow)" />
      <text className="wheel-center" x="120" y="115">Tonight?</text>
      <text className="wheel-center-sub" x="120" y="133">odds preview</text>
    </svg>
  );
}

function Stepper({ w, onChange }: { w: number; onChange: (n: number) => void }) {
  return (
    <span className="stepper">
      <button aria-label="Fewer chances" onClick={() => onChange(w - 1)} disabled={w === 0}>−</button>
      <output className={w === 0 ? "stepper-zero" : ""}>{w}</output>
      <button aria-label="More chances" onClick={() => onChange(w + 1)}>+</button>
    </span>
  );
}

export default function RouletteView({ pickedMovies, weightOf, setWeight, open }: {
  pickedMovies: Movie[];
  weightOf: (id: string) => number;
  setWeight: (id: string, w: number) => void;
  open: (id: string) => void;
}) {
  const weights = useMemo(() => Object.fromEntries(pickedMovies.map(m => [m.id, weightOf(m.id)])), [pickedMovies, weightOf]);
  const [shared, setShared] = useState(false);

  const weighted = pickedMovies.filter(m => weightOf(m.id) > 0);
  const totalWeight = weighted.reduce((sum, m) => sum + weightOf(m.id), 0);
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
          <h2>Pick two or more first.</h2>
          <p>The wheel needs something to spin for. Go back to the <a href="#/">shortlist</a> and pick what you and yours can agree on.</p>
        </div>
      </section>
    );
  }

  return (
    <section className="view roulette">
      <div className="view-head">
        <div>
          <h1>Tonight</h1>
          <p className="view-sub">{pickedMovies.length} in the hat · weights set the odds</p>
        </div>
      </div>
      <div className="roulette-grid">
        <ul className="roulette-list" aria-label="Movies in the pool">
          {pickedMovies.map(m => (
            <li key={m.id} className="roulette-row">
              <button className="roulette-swatch" style={{ background: m.wheel }} aria-hidden="true" onClick={() => open(m.id)} />
              <button className="roulette-title" onClick={() => open(m.id)}>{m.title}</button>
              <span className="roulette-odds" title="Share of the wheel">{odds[m.id]}</span>
              <Stepper w={weightOf(m.id)} onChange={w => setWeight(m.id, w)} />
            </li>
          ))}
        </ul>
        <div className="roulette-stage">
          <Wheel picks={pickedMovies} weights={weights} />
          <div className="handoff-card" role="region" aria-label="Continue to the wheel">
            <p className="handoff-kicker">Odds are set</p>
            <p className="handoff-sub">
              These weights build your wheel config — the spin itself stays on Providence,
              where the draw is as fair as ever.
            </p>
            {totalWeight > 0 ? (
              <a className="btn btn-primary" href="https://providence.jfronny.dev" target="_blank" rel="noreferrer">
                Continue to the wheel <span className="ext-mark" aria-hidden="true">↗</span>
              </a>
            ) : (
              <p className="handoff-sub handoff-hint">Give at least one film a weight to build the wheel.</p>
            )}
          </div>
          <p className="wheel-note">
            Weights are per-movie votes — weight 3 means three on the wheel.
          </p>
        </div>
      </div>

      <div className="share-panel">
        <div className="share-head">
          <h2>Deciding together</h2>
          <p className="muted-note">Scanning opens this pool on their phone. Everyone can add films and veto.</p>
        </div>
        {!shared ? (
          <button className="btn" onClick={() => setShared(true)}>
            Open a shared room <Icons.Ticket width={13} height={13} />
          </button>
        ) : (
          <div className="share-open" role="status">
            <div className="share-qr" dangerouslySetInnerHTML={{ __html: mockQr }} />
            <div>
              <p className="share-code">mmv.amklee.de/roulette — <strong>TONIGHT-77</strong></p>
              <p className="muted-note">2 phones connected, weights drift in live.</p>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
