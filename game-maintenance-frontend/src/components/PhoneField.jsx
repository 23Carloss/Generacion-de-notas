import { useId } from "react";
import { DIAL_CODES } from "../phone";
import Select from "./Select";

export default function PhoneField({ value, onChange, disabled = false }) {
  const id = useId();
  const maxDigits = ["52", "1"].includes(value.dialCode) ? 10 : 15 - value.dialCode.length;
  return (
    <div className="field">
      <label htmlFor={`${id}-number`}>Teléfono</label>
      <div className="phone-field">
        <Select
          label="Lada internacional"
          menuLabel="Selecciona una lada"
          disabled={disabled}
          value={value.dialCode}
          onChange={(dialCode) => onChange({ ...value, dialCode })}
          options={[
            ...(value.dialCode === "" ? [{ value: "", label: "Internacional (+)" }] : []),
            ...DIAL_CODES.map(([code, country]) => ({ value: code, label: `${country} (+${code})` })),
          ]}
        />
        <input id={`${id}-number`} className="input" type="tel" inputMode="numeric"
          autoComplete="tel-national" placeholder={maxDigits === 10 ? "10 dígitos" : "Número sin lada"}
          maxLength={maxDigits} pattern={`[0-9]{7,${maxDigits}}`} required disabled={disabled}
          value={value.number} onChange={(e) => onChange({ ...value, number: e.target.value.replace(/\D/g, "").slice(0, maxDigits) })} />
      </div>
      <span className="hint">Selecciona la lada y escribe el número sin repetirla.</span>
    </div>
  );
}
