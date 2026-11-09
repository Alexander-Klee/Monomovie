/* Screenshot helper: captures desktop + mobile shots of each mockup route. */
import { chromium } from "playwright";
import { mkdirSync } from "fs";

const BASE = "http://localhost:5173";
const OUT = "/tmp/opencode/monomovie-mockups/shots";
mkdirSync(OUT, { recursive: true });

const routes = [
  ["shortlist", "/#/"],
  ["movie", "/#/movie/tm10"],
  ["roulette", "/#/roulette"],
  ["watched", "/#/watched"],
];

const browser = await chromium.launch();
for (const [label, path] of routes) {
  // desktop
  let page = await (await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 })).newPage();
  await page.goto(BASE + path, { waitUntil: "networkidle" });
  await page.waitForTimeout(900);
  await page.screenshot({ path: `${OUT}/${label}-desktop.png`, fullPage: label !== "shortlist" });
  // mobile
  page = await (await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true })).newPage();
  await page.goto(BASE + path, { waitUntil: "networkidle" });
  await page.waitForTimeout(700);
  await page.screenshot({ path: `${OUT}/${label}-mobile.png`, fullPage: label !== "shortlist" });
}
// hover state on a tile (desktop only)
const hover = await (await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 })).newPage();
await hover.goto(BASE + "/#/", { waitUntil: "networkidle" });
await hover.waitForTimeout(800);
const tile = hover.locator(".tile").nth(2);
await tile.hover();
await hover.waitForTimeout(400);
await hover.screenshot({ path: `${OUT}/tile-hover.png` });
await browser.close();
console.log("shots done");
