const fs = require("fs");
const pool = require("/tmp/opencode/monomovie-mockups-data/all.json");
const QR = require("qrcode");
const src = require("/home/johannes/Projects/Entertainment/Monomovie/cached_movies.json");

const SHORTLIST = ["tm10","tm155580","tm122851","tm439291","tm1484207","tm97265","tm60593","tm138641","tm83003"];
const SHORTLIST_AT = {
  tm10: 1761484620, tm155580: 1761402200, tm122851: 1761200000,
  tm439291: 1760900000, tm1484207: 1760800000, tm97265: 1759900000,
  tm60593: 1759400000, tm138641: 1758700000, tm83003: 1758200000,
};
const WATCHED = {
  tm56184: "2026-11-07", tm44313: "2026-10-30", tm362: "2026-10-18",
  tm138000: "2026-10-02", tm128715: "2026-09-13",
};
const EXTRA = [
  "ts226904","ts341776","ts314106","ts256052","ts78125","tm56184","tm44313","tm362",
  "tm138000","tm128715","tm97341","tm167206","ts20573","ts320458","ts22138","tm12694",
  "ts385816","ts258721","ts12059","ts41945","tm72529","ts271787",
];
const ids = [...SHORTLIST, ...Object.keys(WATCHED), ...EXTRA];

const NAME = {
  "Amazon Prime Video": "Prime Video",
  "Amazon Prime Video with Ads": "Prime Video + ads",
  "Netflix Standard with Ads": "Netflix + ads",
  "HBO Max Amazon Channel": "HBO Max + ads",
  "RTL+ Max Amazon Channel": "RTL+ + ads",
  "MGM Plus Amazon Channel": "MGM+ (Am.)",
  "MUBI Amazon Channel": "MUBI (Am.)",
  "Wow Fiction Amazon Channel": "WOW Fiction (Am.)",
  "Paramount+ Amazon Channel": "Paramount+ (Am.)",
  "Kabel Eins Classics Amazon Channel": "Kabel Eins Classics (Am.)",
  "Seven Entertainment Amazon Channel": "Seven Ent. (Am.)",
};
const GENRES = {
  scf: "Sci-Fi", act: "Action", trl: "Thriller", drm: "Drama", cmd: "Comedy",
  hrr: "Horror", rmt: "Romance", fnt: "Fantasy", ani: "Animation", crm: "Crime",
  dcm: "Documentary", adv: "Adventure", mys: "Mystery", wst: "Western",
  wrr: "War", spt: "Sport", his: "History", fam: "Family", msc: "Music",
};
const WHEELCOLORS = ["#F69E45","#E8555A","#5FBF8F","#56B4D3","#9B7EDE","#DE6FA1","#D8B04A","#48A693","#F07B5D","#6E7FD4"];

const movies = ids.map((id, i) => {
  const p = pool.find(x => x.id === id);
  const raw = src[id];
  const objectType = raw?.mediaEntry?.objectType ?? (id.startsWith("ts") ? "SHOW" : "MOVIE");
  return {
    id,
    type: objectType === "TV_SHOW" || id.startsWith("ts") ? "show" : "movie",
    title: p.title, year: p.year, runtime: p.runtime, cert: p.cert || null,
    genres: p.genres.map(g => GENRES[g] || g).slice(0, 3),
    desc: p.desc, imdb: p.imdb, imdbVotes: p.imdbVotes, tmdb: p.tmdb, tomato: p.tomato,
    poster: p.poster ? "https://images.justwatch.com" + p.poster.replace(/\/s718\//, "/s166/") : null,
    posterFull: p.poster ? "https://images.justwatch.com" + p.poster : null,
    backdrop: p.backdrop ? "https://images.justwatch.com" + p.backdrop : null,
    offers: p.offers
      .filter(o => ["FLATRATE","FREE","RENT","BUY"].includes(o.type))
      .map(o => ({ type: o.type.toLowerCase(), provider: NAME[o.provider] || o.provider, icon: o.icon ? "https://images.justwatch.com" + o.icon : null, price: o.price || null })),
    rank: p.chartRank || null,
    wheel: WHEELCOLORS[i % WHEELCOLORS.length],
  };
});

// dedupe provider within monetization type; nicer ordering (flatrate, free, rent, buy)
const order = { flatrate: 0, free: 1, rent: 2, buy: 3 };
for (const m of movies) {
  const seen = new Set();
  m.offers = m.offers
    .sort((a, b) => order[a.type] - order[b.type] || (a.price || "zz").localeCompare(b.price || "zz"))
    .filter(o => { const k = o.type + "|" + o.provider; if (seen.has(k)) return false; seen.add(k); return true; });
}

const header = `// Mock data harvested from the Monomovie cache (June 2025), curated for the redesign mockups.
export type OfferKind = "flatrate" | "free" | "rent" | "buy";
export interface Offer { type: OfferKind; provider: string; icon?: string | null; price?: string | null; }
export interface Movie {
  id: string; type: "movie" | "show"; title: string; year: number; runtime: number;
  cert: string | null; genres: string[]; desc: string;
  imdb: number | null; imdbVotes: number | null; tmdb: number | null; tomato: number | null;
  poster: string | null; posterFull: string | null; backdrop: string | null;
  offers: Offer[]; rank: number | null; wheel: string;
}
export const shortlistAt: Record<string, number> = ${JSON.stringify(SHORTLIST_AT)};
export const watched: Record<string, string> = ${JSON.stringify(WATCHED)};
export const movies: Movie[] = __MOVIES__;
export const byId: Record<string, Movie> = Object.fromEntries(movies.map(m => [m.id, m]));
`;
fs.writeFileSync("src/data.ts", header.replace("__MOVIES__", JSON.stringify(movies)));

QR.toString("http://localhost:5173/#/roulette", { type: "svg", margin: 1, color: { dark: "#EFEBE4", light: "#00000000" } }).then(svg => {
  fs.writeFileSync("src/qr.ts", "// Pre-rendered QR (for the shared-roulette mock)\nexport const mockQr: string = " + JSON.stringify(svg) + ";\n");
  console.log("done:", movies.length, "movies");
});
