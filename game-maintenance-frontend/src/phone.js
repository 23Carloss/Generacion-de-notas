// Catálogo inicial de ladas internacionales. Los números locales antiguos se
// muestran con México como selección inicial, sin modificar el registro al abrirlo.
export const DIAL_CODES = [
  ["52", "México"], ["1", "EE. UU. / Canadá"], ["34", "España"],
  ["54", "Argentina"], ["57", "Colombia"], ["56", "Chile"], ["51", "Perú"],
  ["58", "Venezuela"], ["593", "Ecuador"], ["591", "Bolivia"],
  ["595", "Paraguay"], ["598", "Uruguay"], ["502", "Guatemala"],
  ["503", "El Salvador"], ["504", "Honduras"], ["505", "Nicaragua"],
  ["506", "Costa Rica"], ["507", "Panamá"], ["53", "Cuba"],
];

export function splitPhone(phone = "") {
  const digits = phone.replace(/\D/g, "");
  if (phone.trim().startsWith("+")) {
    const match = DIAL_CODES.find(([code]) => digits.startsWith(code));
    if (match) return { dialCode: match[0], number: digits.slice(match[0].length) };
    // Conserva un prefijo no incluido en el catálogo al editar un registro legado.
    return { dialCode: "", number: digits };
  }
  return { dialCode: "52", number: digits };
}

export function joinPhone({ dialCode, number }) {
  if (!/^\d+$/.test(number) || number.length < 7 || (dialCode + number).length > 15) {
    throw new Error("Escribe un teléfono válido: solo dígitos y hasta 15 incluyendo la lada.");
  }
  if (["52", "1"].includes(dialCode) && number.length !== 10) {
    throw new Error("Esta lada requiere un teléfono de 10 dígitos.");
  }
  return `+${dialCode}${number}`;
}
