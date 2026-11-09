import type { SVGProps } from "react";

const base = (p: SVGProps<SVGSVGElement>) => ({
  width: 18, height: 18, viewBox: "0 0 24 24", fill: "none",
  stroke: "currentColor", strokeWidth: 2, strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const, "aria-hidden": true, ...p,
});

export function Search(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><circle cx="11" cy="11" r="7" /><path d="m21 21-4.3-4.3" /></svg>;
}
export function BookmarkFilled(p: SVGProps<SVGSVGElement>) {
  return <svg {...base({ ...p, fill: "currentColor", stroke: "none" })}><path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.5L5 21V4a1 1 0 0 1 1-1Z" /></svg>;
}
export function BookmarkPlus(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.5L5 21V4a1 1 0 0 1 1-1Z" opacity=".45" /><path d="M12 7v6M9 10h6" /></svg>;
}
export function Eye(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z" /><circle cx="12" cy="12" r="2.6" /></svg>;
}
export function EyeOff(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><path d="M2 12s3.6-6.5 10-6.5c1.8 0 3.4.5 4.7 1.2M22 12s-3.6 6.5-10 6.5c-1.8 0-3.4-.5-4.7-1.2" /><path d="m4 20 16-16" /></svg>;
}
export function X(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><path d="M18 6 6 18M6 6l12 12" /></svg>;
}
export function ArrowUp(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><path d="M12 19V5m-6 6 6-6 6 6" /></svg>;
}
export function Dots(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><circle cx="5" cy="12" r="1.4" fill="currentColor" stroke="none" /><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none" /><circle cx="19" cy="12" r="1.4" fill="currentColor" stroke="none" /></svg>;
}
export function Undo(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><path d="M3 8h10a5 5 0 0 1 0 10H8" /><path d="m7 4-4 4 4 4" /></svg>;
}
export function Ticket(p: SVGProps<SVGSVGElement>) {
  return <svg {...base(p)}><path d="M3 8V6a1 1 0 0 1 1-1h16a1 1 0 0 1 1 1v2a2 2 0 0 0 0 4v2a2 2 0 0 0 0 4v2a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-2a2 2 0 0 0 0-4v-2a2 2 0 0 0 0-4Z" /></svg>;
}

export default {
  Search, BookmarkFilled, BookmarkPlus, Eye, EyeOff, X, ArrowUp, Dots, Undo, Ticket,
};
