// Integration checks: AuditFixture on loopback only, never a deployed API or real DB.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const BASE = 'http://127.0.0.1:18765/GameMaintenance/api';
const SITE = 'http://127.0.0.1:4175';

async function api(path, token, method = 'GET', data) {
  const r = await fetch(BASE + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) }, body: data === undefined ? undefined : JSON.stringify(data) });
  assert.ok(r.ok, `Fixture ${method} ${path}: ${r.status}`);
  return r.status === 204 ? null : r.json();
}
async function auth(email) { return api('/auth/login', null, 'POST', { correo: email, password: 'Audit-only-123!' }); }

(async () => {
  const admin = await auth('audit-admin@example.test'), owner = await auth('audit-user@example.test');
  const original = 'Servicio excelente y entrega puntual. Usuario prueba vive en SECRETO_DOMICILIO. Teléfono 6441234567.';
  const excerpt = 'Servicio excelente y entrega puntual.';
  let reviewedId;
  for (let i = 0; i < 15; i++) {
    const ticket = await api('/resumenes', admin.token, 'POST', {
      cliente: { id: owner.cliente.id }, descripcionProblema: 'SECRETO_PROBLEMA', comentariosCliente: 'SECRETO_COMENTARIO',
      listaDispositivos: [{ modeloDispositivo: 'SECRETO_SERIE', detallesDispositivo: 'SECRETO_DANOS', plataforma: i % 2 ? 'XBOX' : 'PLAYSTATION' }],
      listaTrabajos: [{ tipoTrabajo: 'MANTENIMIENTO', precio: 500 }],
    });
    await api(`/resumenes/${ticket.id}/estado`, admin.token, 'PUT', { estado: 'Entregado' });
    if (i === 14) {
      reviewedId = ticket.id;
      await api(`/resumenes/${ticket.id}/resena`, owner.token, 'PUT', { calificacion: 5, resenaComentario: original });
    }
  }

  for (const channel of ['msedge', 'chrome']) {
    const browser = await chromium.launch({ headless: true, channel });
    try {
      const page = await browser.newPage({ viewport: { width: 1280, height: 900 }, reducedMotion: 'reduce' });
      const errors = [], publicResponses = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('**/*', route => ['127.0.0.1', 'localhost'].includes(new URL(route.request().url()).hostname) ? route.continue() : route.abort());
      page.on('response', async r => {
        if (r.url().includes('/trabajos-resueltos') && r.ok()) publicResponses.push(await r.text().catch(() => ''));
      });
      async function login(email) {
        await page.getByPlaceholder('tu@correo.com').fill(email);
        await page.locator('input[type=password]').fill('Audit-only-123!');
        await page.getByRole('button', { name: 'Entrar', exact: true }).click();
        await page.getByRole('button', { name: 'Abrir menú', exact: true }).waitFor();
        await page.locator('.dashboard-welcome').waitFor();
      }
      async function navigate(name) {
        await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
        await page.getByRole('navigation', { name: 'Apartados' }).getByRole('button', { name, exact: true }).click();
        await page.locator('dialog[open]').waitFor({ state: 'hidden' });
      }
      async function fits(label) {
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), `${channel} ${label}: page must not overflow horizontally`);
      }
      async function logout() {
        await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
        await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
        await page.getByRole('button', { name: 'Entrar', exact: true }).waitFor();
      }

      await page.goto(SITE); await login('audit-admin@example.test');
      await page.evaluate(id => { location.hash = '#ticket/' + id; }, reviewedId);
      await page.getByText(original, { exact: true }).waitFor();
      await page.getByLabel('Fragmento anónimo para mostrar').fill(excerpt);
      assert.equal(await page.getByRole('button', { name: 'Publicar fragmento', exact: true }).isDisabled(), true);
      await page.getByRole('checkbox').check();
      await page.getByRole('button', { name: 'Publicar fragmento', exact: true }).click();
      await page.getByText('Fragmento publicado:', { exact: true }).waitFor();
      for (const width of [320, 390, 768, 1280, 1600]) {
        await page.setViewportSize({ width, height: 900 });
        await fits(`ticket detail ${width}`);
      }
      for (const width of [320, 390, 768, 1280]) {
        await page.setViewportSize({ width, height: 844 });
        await navigate('Clientes'); await page.getByRole('region', { name: 'Lista de clientes' }).waitFor(); await fits(`clients ${width}`);
        await page.getByRole('button', { name: '+ Nuevo cliente', exact: true }).click();
        await page.getByLabel('Nombre', { exact: true }).waitFor(); await fits(`client form ${width}`);
        await navigate('Nuevo ticket'); await page.getByPlaceholder('Modelo (ej. PS4 Slim)').waitFor(); await fits(`ticket form ${width}`);
      }
      await logout(); await login('audit-other@example.test');
      await page.locator('.portfolio-compact .portfolio-card').first().waitFor();
      await page.getByText('“' + excerpt + '”', { exact: true }).waitFor();
      assert.equal(await page.locator('.portfolio-compact').getByText(/SECRETO|audit-user|6441234567/).count(), 0);
      for (const width of [320, 390, 768, 1280, 1600]) {
        await page.setViewportSize({ width, height: 844 });
        await navigate('Panel'); await page.locator('.portfolio-compact .portfolio-card').first().waitFor(); await fits(`dashboard ${width}`);
        await navigate('Trabajos resueltos'); await page.locator('.portfolio-card').first().waitFor(); await fits(`portfolio ${width}`);
        await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
        const drawer = page.getByRole('dialog', { name: 'Menú principal' });
        const box = await drawer.boundingBox(); assert.equal(box.x, 0); assert.ok(box.width < width);
        for (let i = 0; i < 10; i++) {
          await page.keyboard.press('Tab');
          assert.ok(await drawer.evaluate(el => el.contains(document.activeElement)), 'Keyboard focus stays in drawer');
        }
        await page.keyboard.press('Escape');
        assert.equal(await page.getByRole('button', { name: 'Abrir menú', exact: true }).evaluate(el => el === document.activeElement), true);
        await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
        await page.mouse.click(width - 4, 120); await page.locator('dialog[open]').waitFor({ state: 'hidden' });
        if (width === 390 || width === 1280) await page.screenshot({ path: `tests/portfolio-${channel}-${width}.png`, fullPage: true });
      }
      assert.equal(await page.locator('.portfolio-card').count(), 12);
      await page.getByRole('button', { name: 'Siguiente →', exact: true }).click();
      await page.getByText(/Página 2 de/).waitFor();
      await page.getByRole('button', { name: '← Anterior', exact: true }).click();
      await page.getByText(/Página 1 de/).waitFor();
      assert.equal(await page.locator('.portfolio-card a, .portfolio-card button').count(), 0);
      await navigate('Mi perfil'); await page.getByRole('heading', { name: 'Mi perfil', exact: true }).waitFor();
      await page.setViewportSize({ width: 320, height: 568 }); await fits('profile 320');
      await navigate('Mis tickets');
      await page.route('**/trabajos-resueltos?*', route => route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"Fallo simulado de prueba"}' }), { times: 1 });
      await navigate('Trabajos resueltos'); await page.getByRole('alert').waitFor();
      await page.getByRole('button', { name: 'Reintentar', exact: true }).click();
      await page.locator('.portfolio-card').first().waitFor();
      await navigate('Mis tickets');
      await page.route('**/trabajos-resueltos?*', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ trabajos: [], total: 0, pagina: 1, tamano: 12, valoraciones: 0, promedio: null }) }), { times: 1 });
      await navigate('Trabajos resueltos');
      await page.getByRole('heading', { name: 'Los próximos resultados estarán aquí' }).waitFor();
      await page.setViewportSize({ width: 844, height: 390 });
      await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
      await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).scrollIntoViewIfNeeded();
      await page.keyboard.press('Escape'); await fits('landscape menu');
      await page.evaluate(id => { location.hash = '#ticket/' + id; }, reviewedId);
      await page.getByRole('heading', { name: 'Ticket no encontrado', exact: true }).waitFor();
      assert.ok(publicResponses.length > 0);
      for (const payload of publicResponses) assert.doesNotMatch(payload, /SECRETO|audit-user|6441234567|idCliente|descripcionProblema|listaImagenes/);
      assert.deepEqual(errors, []);
      // Reset publication through the actual administrator UI and verify the empty/comment state.
      await logout(); await login('audit-admin@example.test');
      await page.evaluate(id => { location.hash = '#ticket/' + id; }, reviewedId);
      await page.getByRole('button', { name: 'Retirar fragmento', exact: true }).click();
      await page.getByLabel('Fragmento anónimo para mostrar').waitFor();
      const g = await api('/trabajos-resueltos', owner.token);
      assert.equal(g.trabajos[0].resena, null);
      console.log(`PASS ${channel}: real API publication/unpublication, private response allowlist, pagination, error/retry/empty states, left drawer/focus/Escape/backdrop, responsive 320–1600px and landscape, no JS errors.`);
    } finally { await browser.close(); }
  }
})().catch(e => { console.error(e); process.exitCode = 1; });
