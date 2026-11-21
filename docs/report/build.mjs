// Builds report.html from REPORT.md, pulling sections from ../DESIGN.md and ../TEST_CASES.md.
// Then print it to PDF with Chrome (see README in this folder):
//   node build.mjs [groupNumber] && <chrome> --headless --print-to-pdf=Report.pdf report.html
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const read = (p) => readFileSync(join(here, p), 'utf8');
const group = process.argv[2] ?? '____';

// Strip links to repository files (they do not work inside a PDF) but keep their text.
const unlink = (md) => md.replace(/\[([^\]]+)\]\((?!https?:)[^)]+\)/g, '$1');

// DESIGN.md sections are "## N. Title"; return the body of section N with headings demoted one level.
function designSection(n) {
  const lines = read('../DESIGN.md').split('\n');
  const start = lines.findIndex((l) => l.startsWith(`## ${n}. `));
  if (start < 0) throw new Error(`DESIGN.md section ${n} not found`);
  let end = lines.findIndex((l, i) => i > start && l.startsWith('## '));
  if (end < 0) end = lines.length;
  return printFriendly(unlink(lines.slice(start + 1, end)
    .filter((l) => l.trim() !== '---')
    .map((l) => (l.startsWith('### ') ? l.slice(1) : l))
    .join('\n')));
}

// Adjust diagrams for a portrait A4 page: wide left-to-right layouts become top-to-bottom, and ER
// attributes written on one line ("{ a; b }", fine on GitHub) are split onto lines for mermaid 10.
function printFriendly(md) {
  return md
    .replace(/^flowchart LR\n(\s+subgraph Browser)/m, 'flowchart TB\n$1')
    .replace(/^(\s*)direction LR$/m, '$1direction TB')
    .replace(/^classDiagram\n(\s+)class IncidentState \{/m, 'classDiagram\n$1direction LR\n$1class IncidentState {')
    .replace(/^(\s*)([A-Z_]+) \{ (.+) \}$/gm, (_, indent, entity, attrs) =>
      `${indent}${entity} {\n${attrs.split('; ').map((a) => `${indent}    ${a}`).join('\n')}\n${indent}}`);
}

function testsSection() {
  const md = read('../TEST_CASES.md').split('\n').slice(1).join('\n'); // drop "# Test cases"
  return unlink(md).replace(/^## /gm, '## ');
}

const body = read('REPORT.md')
  .replace('{{GROUP}}', group)
  .replace(/\{\{DESIGN:(\d+)\}\}/g, (_, n) => designSection(n))
  .replace('{{TESTS}}', testsSection());

const html = `<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<title>I-ITSM Project Report</title>
<style>
  @page { size: A4; margin: 18mm 16mm 18mm 16mm; }
  body { font: 10.5pt/1.5 "Helvetica Neue", Helvetica, Arial, sans-serif; color: #1d2329; }
  h1 { font-size: 20pt; margin: 0 0 10pt; padding-bottom: 4pt; border-bottom: 2px solid #1d2329; break-before: page; }
  h2 { font-size: 13.5pt; margin: 16pt 0 6pt; color: #24323f; break-after: avoid; }
  h3 { font-size: 11.5pt; margin: 12pt 0 4pt; break-after: avoid; }
  p, li { orphans: 3; widows: 3; }
  table { border-collapse: collapse; width: 100%; margin: 6pt 0 10pt; font-size: 9pt; break-inside: auto; }
  tr { break-inside: avoid; }
  th, td { border: 1px solid #c9d1d9; padding: 3pt 5pt; text-align: left; vertical-align: top; }
  th { background: #f1f4f7; }
  code { font: 8.8pt Menlo, Consolas, monospace; background: #f4f6f8; padding: 0 2pt; border-radius: 2pt; }
  pre { background: #f4f6f8; padding: 6pt 8pt; border-radius: 3pt; overflow: hidden; break-inside: avoid; }
  pre code { background: none; padding: 0; font-size: 8.3pt; }
  .mermaid { text-align: center; margin: 8pt 0 12pt; break-inside: avoid; }
  .mermaid svg { max-width: 100% !important; height: auto; max-height: 235mm; }
  .mermaid.landscape { position: relative; height: 225mm; }
  .mermaid.landscape svg { position: absolute; left: 50%; top: 50%; max-height: none;
    max-width: none !important; height: auto; transform: translate(-50%, -50%) rotate(90deg); }
  a { color: #1f5fbf; text-decoration: none; }
  .cover { height: 250mm; display: flex; flex-direction: column; justify-content: center; }
  .cover h1 { break-before: auto; font-size: 44pt; border: none; margin: 0; letter-spacing: 1pt; }
  .cover h2 { font-size: 17pt; font-weight: 500; margin: 2pt 0 18pt; color: #44515e; }
  .cover .course { font-size: 11pt; color: #5b6773; margin-bottom: 24pt; text-transform: uppercase; letter-spacing: .8pt; }
  .cover .subtitle { font-size: 14pt; margin-bottom: 30pt; }
  .cover .members { font-size: 10pt; }
  .cover .members th { width: 28%; }
  .cover .repo { margin-top: 18pt; }
  .toc { break-before: page; }
  .toc ol { font-size: 12pt; line-height: 2; }
</style></head>
<body>
<div id="content"></div>
<script type="text/markdown" id="src">${body.replace(/<\/script>/g, '<\\/script>')}</script>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>
<script>
  const renderer = new marked.Renderer();
  const plainCode = renderer.code.bind(renderer);
  renderer.code = (code, lang) => lang === 'mermaid'
    ? '<div class="mermaid">' + code.replace(/</g, '&lt;') + '</div>'
    : plainCode(code, lang);
  document.getElementById('content').innerHTML =
    marked.parse(document.getElementById('src').textContent, { renderer });
  mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'loose',
    flowchart: { useMaxWidth: true }, class: { useMaxWidth: true }, er: { useMaxWidth: true } });
  // Very wide diagrams are turned sideways onto their own page so the text stays readable.
  mermaid.run().then(() => {
    document.querySelectorAll('.mermaid svg').forEach((svg) => {
      const box = svg.viewBox.baseVal;
      if (box.width / box.height < 1.6) return;
      svg.parentElement.classList.add('landscape');
      // Keep the diagram's heading on the same page as the diagram.
      const heading = svg.parentElement.previousElementSibling;
      if (heading && /^H[23]$/.test(heading.tagName)) heading.style.breakBefore = 'page';
      svg.removeAttribute('style');
      svg.setAttribute("width", "222mm");
      svg.removeAttribute('height');
    });
    document.body.dataset.ready = '1';
  });
</script>
</body></html>`;

writeFileSync(join(here, 'report.html'), html);
console.log('wrote report.html');
