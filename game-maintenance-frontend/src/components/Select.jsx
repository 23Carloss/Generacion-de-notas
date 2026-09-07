import { useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

function normalize(text) {
  return text.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("es");
}

// Keeps focus on the combobox while arrows explore options; only an explicit
// selection calls onChange (important for selectors that save immediately).
export default function Select({ id, label, menuLabel = label, value, onChange, options, disabled = false }) {
  const generatedId = useId();
  const controlId = id || generatedId;
  const listId = `${controlId}-list`;
  const triggerRef = useRef(null);
  const menuRef = useRef(null);
  const listRef = useRef(null);
  const searchRef = useRef({ text: "", time: 0 });
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [position, setPosition] = useState(null);
  const selectedIndex = options.findIndex((option) => String(option.value) === String(value));
  const unavailable = disabled || !options.length;
  const expanded = open && !unavailable;

  useLayoutEffect(() => {
    if (!expanded) return;

    function updatePosition() {
      const rect = triggerRef.current.getBoundingClientRect();
      const margin = 8;
      const gap = 6;
      const below = window.innerHeight - rect.bottom - margin - gap;
      const above = rect.top - margin - gap;
      const upwards = below < 280 && above > below;
      const width = Math.min(rect.width, window.innerWidth - margin * 2);
      setPosition({
        width,
        left: Math.max(margin, Math.min(rect.left, window.innerWidth - width - margin)),
        ...(upwards ? { bottom: window.innerHeight - rect.top + gap } : { top: rect.bottom + gap }),
        maxHeight: Math.min(280, Math.max(0, upwards ? above : below)),
      });
    }

    function handleScroll(event) {
      if (!menuRef.current?.contains(event.target)) updatePosition();
    }

    updatePosition();
    window.addEventListener("resize", updatePosition);
    window.addEventListener("scroll", handleScroll, true);
    return () => {
      window.removeEventListener("resize", updatePosition);
      window.removeEventListener("scroll", handleScroll, true);
    };
  }, [expanded]);

  useEffect(() => {
    if (!expanded) return;
    function closeOutside(event) {
      if (!triggerRef.current?.contains(event.target) && !menuRef.current?.contains(event.target)) {
        setOpen(false);
      }
    }
    document.addEventListener("pointerdown", closeOutside);
    document.addEventListener("focusin", closeOutside);
    return () => {
      document.removeEventListener("pointerdown", closeOutside);
      document.removeEventListener("focusin", closeOutside);
    };
  }, [expanded]);

  useLayoutEffect(() => {
    if (!expanded || !position) return;
    const list = listRef.current;
    const option = list?.children[activeIndex];
    if (!option) return;
    // Scroll only the list, never the surrounding form or page.
    const top = option.offsetTop;
    const bottom = top + option.offsetHeight;
    if (top < list.scrollTop) list.scrollTop = top;
    else if (bottom > list.scrollTop + list.clientHeight) list.scrollTop = bottom - list.clientHeight;
  }, [expanded, activeIndex, position]);

  function show(direction = 1) {
    if (unavailable) return;
    searchRef.current = { text: "", time: 0 };
    setActiveIndex(selectedIndex >= 0 ? selectedIndex : direction > 0 ? 0 : options.length - 1);
    setOpen(true);
  }

  function choose(index) {
    const option = options[index];
    if (!option) return;
    setOpen(false);
    triggerRef.current?.focus({ preventScroll: true });
    if (index !== selectedIndex) onChange(option.value);
  }

  function handleKeyDown(event) {
    if (unavailable) return;
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      const step = event.key === "ArrowDown" ? 1 : -1;
      if (!expanded) show(step);
      else setActiveIndex((index) => Math.max(0, Math.min(options.length - 1, index + step)));
    } else if (["Home", "End", "PageDown", "PageUp"].includes(event.key)) {
      event.preventDefault();
      if (!expanded) show();
      setActiveIndex((index) => event.key === "Home" ? 0 : event.key === "End" ? options.length - 1
        : Math.max(0, Math.min(options.length - 1, index + (event.key === "PageDown" ? 5 : -5))));
    } else if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      if (expanded) choose(activeIndex);
      else show();
    } else if (event.key === "Escape") {
      if (expanded) event.preventDefault();
      setOpen(false);
    } else if (event.key === "Tab") {
      setOpen(false);
    } else if (event.key.length === 1 && !event.ctrlKey && !event.altKey && !event.metaKey) {
      event.preventDefault();
      const now = Date.now();
      const text = (now - searchRef.current.time < 700 ? searchRef.current.text : "") + normalize(event.key);
      searchRef.current = { text, time: now };
      const query = [...text].every((letter) => letter === text[0]) ? text[0] : text;
      const start = expanded ? activeIndex : selectedIndex;
      for (let offset = 1; offset <= options.length; offset++) {
        const index = (start + offset + options.length) % options.length;
        if (normalize(options[index].label).startsWith(query)) {
          setActiveIndex(index);
          setOpen(true);
          break;
        }
      }
    }
  }

  return (
    <div className="select-control">
      <button
        ref={triggerRef}
        id={controlId}
        type="button"
        role="combobox"
        className="select-trigger"
        aria-label={label}
        aria-haspopup="listbox"
        aria-expanded={expanded}
        aria-controls={expanded ? listId : undefined}
        aria-activedescendant={expanded && options[activeIndex] ? `${listId}-${activeIndex}` : undefined}
        disabled={unavailable}
        onClick={() => expanded ? setOpen(false) : show()}
        onKeyDown={handleKeyDown}
      >
        <span className="select-value">{options[selectedIndex]?.label || (options.length ? menuLabel : "Sin opciones disponibles")}</span>
        <svg className="select-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
      {expanded && position && createPortal(
        <div ref={menuRef} className="select-menu" style={position}>
          <div className="select-menu-title">{menuLabel}</div>
          <div ref={listRef} id={listId} className="select-options" role="listbox" aria-label={label}>
            {options.map((option, index) => (
              <div
                key={option.value}
                id={`${listId}-${index}`}
                role="option"
                aria-selected={index === selectedIndex}
                className={`select-option${index === activeIndex ? " is-active" : ""}`}
                onPointerDown={(event) => { if (event.pointerType === "mouse") event.preventDefault(); }}
                onClick={() => choose(index)}
              >
                <svg className="select-check" width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  {index === selectedIndex && <path d="m5 12 4 4L19 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />}
                </svg>
                <span>{option.label}</span>
              </div>
            ))}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
