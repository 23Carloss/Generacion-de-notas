import { test } from 'node:test';
import assert from 'node:assert/strict';
import { prepareWorks, validateDevices } from '../src/formValidation.js';
import { joinPhone, splitPhone } from '../src/phone.js';

test('repair limits include both boundaries and reject infinity, blanks and fractions', () => {
  const base = { tipoTrabajo: 'REPARACION', nombrePieza: 'p'.repeat(75), unidades: 50, precioUnitario: 50_000 };
  assert.equal(prepareWorks([base])[0].precio, 2_500_000);
  assert.equal(prepareWorks([{ ...base, unidades: 1, precioUnitario: 0 }])[0].precio, 0);
  for (const value of [51, 0, -1, 1.5, Infinity, NaN, '', null, '1e999']) {
    assert.throws(() => prepareWorks([{ ...base, unidades: value }]));
  }
  for (const value of [50_000.01, -1, Infinity, NaN, '', null, '1e999']) {
    assert.throws(() => prepareWorks([{ ...base, precioUnitario: value }]));
  }
  assert.throws(() => prepareWorks([{ ...base, nombrePieza: 'p'.repeat(76) }]));
});
test('device name and optional details limits', () => {
  const base = { modeloDispositivo: 'm'.repeat(50), detallesDispositivo: 'd'.repeat(150) };
  validateDevices([base]); validateDevices([{ ...base, detallesDispositivo: null }]);
  assert.throws(() => validateDevices([{ ...base, modeloDispositivo: 'm'.repeat(51) }]));
  assert.throws(() => validateDevices([{ ...base, detallesDispositivo: 'd'.repeat(151) }]));
});
test('lada roundtrip, local legacy display, and invalid phone input', () => {
  assert.deepEqual(splitPhone('6441234567'), { dialCode: '52', number: '6441234567' });
  for (const number of ['+526441234567', '+14155552671', '+34912345678', '+50212345678', '+441234567890']) {
    assert.equal(joinPhone(splitPhone(number)), number);
  }
  for (const number of ['abc1234567', '123', '1234567890123456']) {
    assert.throws(() => joinPhone({ dialCode: '52', number }));
  }
});
