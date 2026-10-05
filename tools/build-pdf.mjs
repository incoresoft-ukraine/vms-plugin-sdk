// Builds a single-file HTML + PDF from README.md and docs/*.md.
// Markdown → HTML via micromark (+ GFM tables) taken from a local node_modules;
// HTML → PDF via headless Google Chrome. No network access needed.
import { readFileSync, writeFileSync, readdirSync, mkdirSync } from 'node:fs'
import { pathToFileURL } from 'node:url'
import { execFileSync } from 'node:child_process'
import path from 'node:path'

const SDK = path.resolve(new URL('..', import.meta.url).pathname)
// micromark is not a dependency of the SDK itself; point MICROMARK_NODE_MODULES at any
// node_modules that contains micromark + micromark-extension-gfm (e.g. the VMS frontend workspace).
import { existsSync } from 'node:fs'
const NM_CANDIDATES = [process.env.MICROMARK_NODE_MODULES, path.resolve(SDK, 'node_modules')].filter(Boolean)
const NM = NM_CANDIDATES.find(p => existsSync(path.join(p, 'micromark/index.js')))
if (!NM) throw new Error('micromark not found; set MICROMARK_NODE_MODULES')
const { micromark } = await import(pathToFileURL(path.join(NM, 'micromark/index.js')).href)
const { gfm, gfmHtml } = await import(pathToFileURL(path.join(NM, 'micromark-extension-gfm/index.js')).href)

const md = (src) => micromark(src, { extensions: [gfm()], htmlExtensions: [gfmHtml()], allowDangerousHtml: false })

const slug = (s) => s.toLowerCase().replace(/<[^>]+>/g, '').replace(/&[a-z]+;/g, '').replace(/[^a-z0-9а-яіїєґ]+/gi, '-').replace(/^-|-$/g, '')


const css = `
@page { size: A4; margin: 16mm 14mm 18mm 14mm; }
@page wide { size: A4 landscape; margin: 14mm 14mm 16mm 14mm; }
:root { color-scheme: light; }
html { font-size: 10.5pt; }
body { margin: 0; color: #1a1a1a; background: #fff; font-family: -apple-system, "Helvetica Neue", Helvetica, Arial, sans-serif; line-height: 1.45; }
.cover { height: 250mm; display: flex; flex-direction: column; justify-content: center; break-after: page; }
.cover h1 { font-size: 34pt; margin: 0 0 6mm; letter-spacing: -0.5px; }
.cover .sub { font-size: 15pt; color: #444; margin-bottom: 18mm; }
.cover .meta { color: #666; font-size: 10pt; line-height: 1.8; }
.cover .meta b { color: #222; }
.toc { break-after: page; }
.toc ul { list-style: none; padding: 0; margin: 0; }
.toc li { margin: 1.5px 0; }
.toc li.lvl1 { margin-top: 8px; font-weight: 600; }
.toc li.lvl2 { padding-left: 14px; font-size: 9.5pt; }
.toc a { color: #1a1a1a; text-decoration: none; }
section.doc { break-before: page; }
section.doc:first-of-type { break-before: auto; }
h1 { font-size: 20pt; margin: 0 0 8pt; padding-bottom: 4pt; border-bottom: 2px solid #1f5aa6; color: #12345f; break-after: avoid; }
h2 { font-size: 14.5pt; margin: 18pt 0 6pt; color: #1f5aa6; break-after: avoid; }
h3 { font-size: 12pt; margin: 14pt 0 4pt; break-after: avoid; }
p, ul, ol { margin: 5pt 0; }
li { margin: 1.5pt 0; }
a { color: #1f5aa6; word-break: break-all; }
code, pre { font-family: Menlo, "SF Mono", Consolas, monospace; font-size: 8.6pt; }
code { background: #f1f4f8; padding: 0.5px 3px; border-radius: 3px; word-break: break-word; }
pre { background: #f5f7fa; border: 1px solid #dde3ea; border-radius: 4px; padding: 7pt 9pt; white-space: pre-wrap; word-break: break-word; overflow-wrap: anywhere; margin: 6pt 0; break-inside: avoid; }
pre code { background: none; padding: 0; }
blockquote { margin: 6pt 0; padding: 4pt 10pt; border-left: 3px solid #1f5aa6; background: #f5f8fc; color: #333; }
table { border-collapse: collapse; width: 100%; margin: 6pt 0 8pt; font-size: 8.6pt; table-layout: auto; }
thead { display: table-header-group; }
tr { break-inside: avoid; }
th, td { border: 1px solid #cfd6de; padding: 3.5pt 5pt; vertical-align: top; text-align: left; word-break: break-word; overflow-wrap: anywhere; }
th { background: #e9eef5; font-weight: 600; }
td code, th code { font-size: 8pt; }
td:first-child code, td:nth-child(2) code { word-break: keep-all; overflow-wrap: normal; white-space: nowrap; }
hr { border: 0; border-top: 1px solid #cfd6de; margin: 10pt 0; }
img { max-width: 100%; }
`

// ---- build one document --------------------------------------------------------------------
function build(opts) { return buildTo(SDK, opts) }
function buildTo(outDir, { name, title, subtitle, files, landscapeIds = ['doc-04'], lang = 'en' }) {
  let body = ''
  const toc = []
  for (const { id, file } of files) {
    let src = readFileSync(file, 'utf8')
    src = src.replace(/\]\((?:docs\/)?(\d\d)-[^)#]+\.md\)/g, '](#doc-$1)')
    src = src.replace(/\]\(README\.md\)/g, '](#readme)')
    let html = md(src)
    html = html.replace(/<h([1-3])>(.*?)<\/h\1>/g, (m, lvl, text) => {
      const anchor = `${id}-${slug(text)}`
      if (lvl <= 2) toc.push({ lvl: +lvl, text, anchor, id })
      return `<h${lvl} id="${anchor}">${text}</h${lvl}>`
    })
    body += `<section class="doc" id="${id}">${html}</section>\n`
  }
  const tocHtml = files.length > 1
    ? '<nav class="toc"><h1>' + (lang === 'uk' ? 'Зміст' : 'Contents') + '</h1><ul>' + toc.map(t =>
        `<li class="lvl${t.lvl}"><a href="#${t.anchor}">${t.text}</a></li>`).join('') + '</ul></nav>'
    : ''
  const today = new Date().toISOString().slice(0, 10)
  const landscapeCss = landscapeIds.map(id => `section#${id} { page: wide; } section#${id} table { font-size: 8.2pt; }`).join('\n')
  const cover = `<div class="cover">
  <h1>${title}</h1>
  <div class="sub">${subtitle}</div>
  <div class="meta"><b>${lang === 'uk' ? 'Цільова лінія релізів' : 'Target release line'}:</b> VMS 25.1 (Javalin 5, Java 21, Vue 3.4, Module Federation)<br>
  <b>${lang === 'uk' ? 'Дата' : 'Generated'}:</b> ${today}</div>
</div>`
  const html = `<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><title>${title.replace(/<br>/g, ' ')}</title><style>${css}\n${landscapeCss}</style></head><body>${cover}${tocHtml}${body}</body></html>`
  mkdirSync(path.join(outDir, 'build'), { recursive: true })
  const htmlPath = path.join(outDir, 'build', `${name}.html`)
  writeFileSync(htmlPath, html)
  const pdfPath = path.join(outDir, `${name}.pdf`)
  const chrome = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
  execFileSync(chrome, ['--headless', '--disable-gpu', '--no-sandbox', '--run-all-compositor-stages-before-draw',
    '--virtual-time-budget=15000', '--no-pdf-header-footer', `--print-to-pdf=${pdfPath}`, pathToFileURL(htmlPath).href],
    { stdio: 'ignore' })
  const pdf = readFileSync(pdfPath)
  const pages = (pdf.toString('latin1').match(/\/Type\s*\/Page(?!s)/g) || []).length
  console.log(`${name}.pdf  ${(pdf.length / 1024).toFixed(0)} KB  ${pages} pages  (${files.length} sources)`)
}

const docsDir = path.join(SDK, 'docs')
const docFiles = (filter) => readdirSync(docsDir).filter(f => /^\d\d-.*\.md$/.test(f) && filter(f)).sort()
  .map(f => ({ id: 'doc-' + f.slice(0, 2) + (f.includes('.uk.') ? '-uk' : ''), file: path.join(docsDir, f) }))

// 1. Partner deliverable: README + 00 (EN) + 01..11
build({
  name: 'VMS-Plugin-SDK-Developer-Guide',
  title: 'Incoresoft VMS<br>Plugin SDK',
  subtitle: 'Developer Guide & Reference',
  files: [{ id: 'readme', file: path.join(SDK, 'README.md') }, ...docFiles(f => !f.includes('.uk.'))],
})
// 2. Two-page overview, EN and UK
build({ name: 'VMS-Plugin-SDK-Overview', title: 'Incoresoft VMS<br>Plugin SDK', subtitle: 'Overview for technical decision makers',
  files: docFiles(f => f.startsWith('00-') && !f.includes('.uk.')), landscapeIds: [] })
