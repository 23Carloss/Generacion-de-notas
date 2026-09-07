const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const root = path.resolve(__dirname, '../..');
const paths = execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z'], { cwd: root, encoding: 'utf8' }).split('\0').filter(Boolean);
let mismatches = 0;
for (const file of paths.filter(f => /^game-maintenance-frontend\/src\/.*\.[jt]sx?$/.test(f))) {
  const code = fs.readFileSync(path.join(root, file), 'utf8');
  for (const [, ref] of code.matchAll(/(?:from\s+|import\s*)["'](\.[^"']+)["']/g)) {
    const base = path.posix.normalize(path.posix.join(path.posix.dirname(file), ref));
    const choices = [base, base + '.js', base + '.jsx', base + '/index.js'];
    if (!choices.some(c => paths.includes(c))) {
      console.log('IMPORT CASE/MISSING:', file, ref); mismatches++;
    }
  }
}
console.log('Case-sensitive Git import mismatches:', mismatches);
for (const file of ['Models', 'Persistencia'].map(m => `Game_Maintenance/${m}/src/main/resources/META-INF/persistence.xml`)) {
  const xml = fs.readFileSync(path.join(root, file), 'utf8');
  const props = Object.fromEntries([...xml.matchAll(/<property\s+name="([^"]+)"\s+value="([^"]*)"/g)].map(m => [m[1], m[2]]));
  // Deliberately print only flags, NEVER credential values or connection URLs.
  console.log(JSON.stringify({ file, tracked: paths.includes(file), literalPassword: Boolean(props['javax.persistence.jdbc.password'] && !props['javax.persistence.jdbc.password'].startsWith('${')), rootUser: props['javax.persistence.jdbc.user'] === 'root', schemaCreate: props['javax.persistence.schema-generation.database.action'] === 'create' }));
}
if (mismatches) process.exitCode = 1;
