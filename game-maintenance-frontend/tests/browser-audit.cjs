// Run against AuditFixture ONLY: disposable H2 data on loopback, no real accounts.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');

(async () => {
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    const errors = [], dialogs = [];
    page.on('pageerror', e => errors.push(e.message));
    page.on('dialog', async d => { dialogs.push(d.message()); await d.dismiss(); });
    // Block external traffic (including fonts) during this isolated UI test.
    await page.route('**/*', route => {
      const url = new URL(route.request().url());
      return ['127.0.0.1', 'localhost'].includes(url.hostname) ? route.continue() : route.abort();
    });
    async function choose(label, option) {
      const field = page.getByRole('combobox', { name: label, exact: true });
      if (await field.evaluate(el => el.tagName === 'SELECT')) await field.selectOption({ label: option });
      else { await field.click(); await page.getByRole('option', { name: option, exact: true }).click(); }
    }
    async function login(email) {
      await page.getByPlaceholder('tu@correo.com').fill(email);
      await page.locator('input[type=password]').fill('Audit-only-123!');
      await page.getByRole('button', { name: 'Entrar', exact: true }).click();
      await page.getByRole('button', { name: 'Abrir menú', exact: true }).waitFor();
    }
    async function navigate(name) {
      await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
      await page.getByRole('navigation', { name: 'Apartados' }).getByRole('button', { name, exact: true }).click();
    }
    await page.goto('http://127.0.0.1:4175');
    await login('audit-admin@example.test');
    await navigate('Clientes');
    const search = page.getByRole('textbox', { name: 'Buscar clientes' });
    await search.fill('q'.repeat(121)); assert.equal((await search.inputValue()).length, 120);
    await search.fill('');
    await page.getByRole('button', { name: '+ Nuevo cliente', exact: true }).click();
    const name = page.getByLabel('Nombre', { exact: true });
    await name.fill('n'.repeat(101)); assert.equal((await name.inputValue()).length, 100);
    const xss = '<img src=x onerror=alert(1)>';
    await name.fill(xss);
    await choose('Lada internacional', 'EE. UU. / Canadá (+1)');
    await page.getByLabel('Teléfono', { exact: true }).fill('415ABC5552671');
    assert.match(await page.getByLabel('Teléfono', { exact: true }).inputValue(), /^[0-9]+$/);
    await page.getByLabel('Teléfono', { exact: true }).fill('4155552671');
    await page.getByRole('button', { name: 'Guardar cliente', exact: true }).click();
    const row = page.getByRole('row').filter({ has: page.getByRole('cell', { name: xss, exact: true }) });
    await row.waitFor(); assert.equal(await row.locator('img').count(), 0);
    assert.match(await row.innerText(), /\+14155552671/);
    await row.getByRole('button', { name: 'Editar', exact: true }).click();
    await name.fill('Cliente UI editado');
    await choose('Lada internacional', 'México (+52)');
    await page.getByLabel('Teléfono', { exact: true }).fill('6441234567');
    await page.getByRole('button', { name: 'Guardar cliente', exact: true }).click();
    await page.getByRole('cell', { name: 'Cliente UI editado', exact: true }).waitFor();
    console.log('PASS: búsqueda 120, nombre 100, lada, teléfono numérico, edición, XSS mostrado como texto.');

    await navigate('Nuevo ticket');
    await choose('Selecciona cliente', 'Cliente UI editado · +526441234567');
    const model = page.getByPlaceholder('Modelo (ej. PS4 Slim)');
    const details = page.getByPlaceholder('Detalles / accesorios / daños visibles');
    await model.fill('m'.repeat(51)); assert.equal((await model.inputValue()).length, 50);
    await details.fill('d'.repeat(151)); assert.equal((await details.inputValue()).length, 150);
    await page.getByPlaceholder('Qué reporta el cliente…').fill("' OR '1'='1");
    await page.getByRole('button', { name: '+ Agregar trabajo', exact: true }).click();
    await choose('Tipo de trabajo 1', 'Reparación');
    const part = page.getByPlaceholder('Nombre de la pieza');
    await part.fill('p'.repeat(76)); assert.equal((await part.inputValue()).length, 75);
    await page.getByPlaceholder('Unidades', { exact: true }).fill('51');
    await page.getByPlaceholder('Precio por unidad', { exact: true }).fill('50000');
    await page.getByRole('button', { name: 'Crear ticket', exact: true }).click();
    await page.getByText('Las unidades deben ser un número entero entre 1 y 50.').waitFor();
    await page.getByPlaceholder('Unidades', { exact: true }).fill('50');
    await page.getByPlaceholder('Precio por unidad', { exact: true }).fill('50000.01');
    await page.getByRole('button', { name: 'Crear ticket', exact: true }).click();
    await page.getByText('El precio por unidad debe estar entre 0 y 50,000 MXN.').waitFor();
    await page.getByPlaceholder('Precio por unidad', { exact: true }).fill('50000');
    await page.getByRole('button', { name: 'Crear ticket', exact: true }).click();
    await page.getByRole('button', { name: 'Editar ticket', exact: true }).waitFor();
    await page.getByRole('button', { name: 'Editar ticket', exact: true }).click();
    assert.equal(await page.getByPlaceholder('Modelo', { exact: true }).getAttribute('maxlength'), '50');
    assert.equal(await page.getByPlaceholder('Detalles / accesorios', { exact: true }).getAttribute('maxlength'), '150');
    await page.getByPlaceholder('Unidades', { exact: true }).fill('1');
    await page.getByRole('button', { name: 'Guardar cambios', exact: true }).click();
    await page.getByRole('button', { name: 'Editar ticket', exact: true }).waitFor();
    console.log('PASS: dispositivo 50/150, pieza 75, unidades/precio máximos, creación y edición persistidas en API.');

    await navigate('Clientes');
    await page.getByRole('button', { name: '+ Nuevo cliente', exact: true }).click();
    await page.setViewportSize({ width: 390, height: 844 });
    await page.locator('.phone-field').scrollIntoViewIfNeeded();
    const box = await page.locator('.phone-field').boundingBox();
    assert.ok(box.width > 100 && box.x >= 0 && box.x + box.width <= 390, 'Phone form fits mobile viewport');
    await page.screenshot({ path: 'tests/phone-mobile.png' });
    await page.setViewportSize({ width: 1280, height: 900 });
    await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await login('audit-user@example.test');
    assert.equal(await page.getByRole('button', { name: 'Clientes', exact: true }).count(), 0);
    await page.evaluate(() => { location.hash = '#clientes'; });
    await page.waitForTimeout(200);
    assert.equal(await page.getByRole('button', { name: '+ Nuevo cliente', exact: true }).count(), 0);
    assert.deepEqual(errors, []); assert.deepEqual(dialogs, []);
    console.log('PASS: formulario móvil, controles de administrador ocultos al usuario, sin errores JS ni ejecución XSS.');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
