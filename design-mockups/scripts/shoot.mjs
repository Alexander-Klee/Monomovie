// Screenshot helper: captures desktop + mobile shots of each mockup route.
import { chromium } from "playwright";
import { mkdirSync } from "fs";

const BASE = "http://localhost:5173";
const OUT = new URL("../shots/", import.meta.url).pathname;
mkdirSync(OUT, { recursive: true });

const routes = [
  ["shortlist", "/#/"],
  ["movie", "/#/movie/tm10"],
  ["roulette", "/#/roulette"],
  ["watched", "/#/watched"],
];

const browser = await chromium.launch();

async function shoot(label, path, { mobile = false, fullPage = false, after } = {}) {
  const ctx = await browser.newContext(
    mobile
      ? { viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true }
      : { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 },
  );
  const page = await ctx.newPage();
  await page.goto(BASE + path, { waitUntil: "networkidle" });
  await page.waitForTimeout(mobile ? 700 : 900);
  if (after) await after(page);
  await page.screenshot({ path: `${OUT}/${label}.png`, fullPage });
  await ctx.close();
}

for (const [label, path] of routes) {
  await shoot(`${label}-desktop`, path, { fullPage: label !== "shortlist" });
  await shoot(`${label}-mobile`, path, { mobile: true, fullPage: label !== "shortlist" });
}

// hover state on a tile (desktop only)
await shoot("tile-hover", "/#/", {
  after: async page => {
    await page.locator(".tile").nth(2).hover();
    await page.waitForTimeout(400);
  },
});

// open search dropdown (desktop only)
await shoot("search-open", "/#/", {
  after: async page => {
    await page.keyboard.press("/");
    await page.waitForTimeout(200);
    await page.keyboard.type("god");
    await page.waitForTimeout(800);
  },
});

// roulette: open the shared room
await shoot("roulette-shared", "/#/roulette", {
  fullPage: true,
  after: async page => {
    await page.getByRole("button", { name: /share/i }).click();
    await page.waitForTimeout(700);
  },
});

await browser.close();
console.log("shots done");
