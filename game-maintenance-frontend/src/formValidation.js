export const LIMITS = Object.freeze({
  search: 120, clientName: 100, deviceName: 50, deviceDetails: 150,
  partName: 75, units: 50, unitPrice: 50_000,
});

export function validateDevices(dispositivos) {
  if (!dispositivos.length) throw new Error("Agrega al menos un dispositivo con su modelo.");
  for (const d of dispositivos) {
    if (!d.modeloDispositivo.trim() || d.modeloDispositivo.trim().length > LIMITS.deviceName) {
      throw new Error("El modelo del dispositivo debe tener entre 1 y 50 caracteres.");
    }
    if ((d.detallesDispositivo || "").trim().length > LIMITS.deviceDetails) {
      throw new Error("Los detalles del dispositivo permiten hasta 150 caracteres.");
    }
  }
}

export function prepareWorks(trabajos) {
  return trabajos.flatMap((t) => {
    if (t.tipoTrabajo === "REPARACION") {
      const nombrePieza = (t.nombrePieza || "").trim();
      const unidades = Number(t.unidades);
      const precioUnitario = Number(t.precioUnitario);
      if (!nombrePieza || nombrePieza.length > LIMITS.partName) {
        throw new Error("El nombre de la pieza debe tener entre 1 y 75 caracteres.");
      }
      if (t.unidades === "" || t.unidades == null || !Number.isInteger(unidades) || unidades < 1 || unidades > LIMITS.units) {
        throw new Error("Las unidades deben ser un número entero entre 1 y 50.");
      }
      if (t.precioUnitario === "" || t.precioUnitario == null || !Number.isFinite(precioUnitario) || precioUnitario < 0 || precioUnitario > LIMITS.unitPrice) {
        throw new Error("El precio por unidad debe estar entre 0 y 50,000 MXN.");
      }
      return [{ tipoTrabajo: t.tipoTrabajo, nombrePieza, unidades, precioUnitario, precio: unidades * precioUnitario }];
    }
    if (t.precio === "" || t.precio == null) return [];
    const precio = Number(t.precio);
    if (!Number.isFinite(precio) || precio < 0 || precio > 1_000_000) {
      throw new Error("El precio del trabajo debe estar entre 0 y 1,000,000 MXN.");
    }
    return [{ tipoTrabajo: t.tipoTrabajo, precio }];
  });
}
