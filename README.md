# Pointeo agency pack — how to add an agency

Pointeo downloads `agencies.json` (and the logos it lists) when the phone is online,
keeps a copy on the phone, and works offline afterwards. No user data is ever sent.

## One-time setup
1. Put this folder on any static host (e.g. GitHub Pages of your `pointeo` repo -> `docs/` or a `gh-pages` branch).
2. In the app source, set `PACK_URL` to the public address of `agencies.json`
   (e.g. `https://YOURNAME.github.io/pointeo/agencies.json`).
   The host must allow cross-origin reads (GitHub Pages and Cloudflare Pages do by default).

## Add an agency (2 minutes)
1. Drop the logo in `logos/` — SVG, or PNG/JPEG/WebP, about 600x240 px, under 150 KB.
2. Add an entry to `agencies.json`:
   { "id": "my-agency", "name": "My Agency", "city": "Lausanne", "fax": "+41 21 555 00 00",
     "logo": "logos/my-agency.svg", "lv": "1" }
   - `id`: lowercase letters, digits, dashes; never change it once published (saved timesheets use it).
   - `lv`: logo version. Change it (e.g. "2") when you replace a logo so phones re-download it.
3. Raise `"version"` at the top by 1 and publish. Phones pick it up the next time they open the app online.

## Good to know
- No logo or a broken link -> the app shows a coloured initial instead (optional `"color": "#1d4ed8"`).
- Invalid entries are skipped silently; the rest still load.
- An agency you remove disappears from the list, except on phones that already used it (so old timesheets still open).
- Get the agency's written permission to use its logo.
- Only you should be able to edit the files: protect the repo / hosting account.
