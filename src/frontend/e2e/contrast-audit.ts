import type { Page } from '@playwright/test';

// Measure actual browser-computed colors. Alpha backgrounds are composited through
// ancestors (including the modal backdrop), rather than assuming a white canvas.
export async function audit(page: Page, mode: 'text' | 'controls' | 'focus') {
  return page.evaluate(mode => {
    type Color = [number, number, number, number];
    const parse = (value: string): Color => {
      const parts = value.match(/[\d.]+/g)?.map(Number) ?? [];
      if (parts.length < 3) throw new Error(`Unsupported computed color: ${value}`);
      return [parts[0], parts[1], parts[2], parts[3] ?? 1];
    };
    const over = (a: Color, b: Color): Color => {
      const alpha = a[3] + b[3] * (1 - a[3]);
      return [0, 1, 2].map(i => (a[i] * a[3] + b[i] * b[3] * (1 - a[3])) / alpha).concat(alpha) as Color;
    };
    const background = (element: Element | null): Color => element ? over(parse(getComputedStyle(element).backgroundColor), background(element.parentElement)) : [255, 255, 255, 1];
    const lum = (c: Color) => c.slice(0, 3).map(v => { const x = v / 255; return x <= .04045 ? x / 12.92 : ((x + .055) / 1.055) ** 2.4; }).reduce((s, x, i) => s + x * [.2126, .7152, .0722][i], 0);
    const ratio = (a: Color, b: Color) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + .05) / (Math.min(x, y) + .05); };
    const visible = (el: HTMLElement) => el.checkVisibility({ checkOpacity: true, checkVisibilityCSS: true }) && el.getBoundingClientRect().width > 0 && !el.closest('[inert],[aria-hidden="true"]');
    const label = (el: HTMLElement) => `${el.tagName.toLowerCase()} ${el.getAttribute('aria-label') || el.textContent?.trim().slice(0, 65) || el.id}`;
    const failures: string[] = []; let checked = 0;
    const check = (el: HTMLElement, kind: string, actual: number, threshold: number) => {
      checked++; if (actual + .01 < threshold) failures.push(`${label(el)} ${kind}: ${actual.toFixed(2)} < ${threshold}`);
    };
    const dialog = document.querySelector('[role="dialog"]');
    const all = Array.from((dialog ?? document.body).querySelectorAll<HTMLElement>('*')).filter(visible);
    if (mode === 'text') for (const el of all) {
      if (el.matches('option,script,style,svg,svg *') || el.matches(':disabled') || el.closest(':disabled')) continue;
      const directText = Array.from(el.childNodes).some(node => node.nodeType === Node.TEXT_NODE && node.textContent?.trim());
      if (!directText && !el.matches('input,select,textarea')) continue;
      const s = getComputedStyle(el); const bg = background(el);
      const large = parseFloat(s.fontSize) >= 24 || (parseFloat(s.fontSize) >= 18.67 && Number(s.fontWeight) >= 700);
      check(el, 'text', ratio(over(parse(s.color), bg), bg), large ? 3 : 4.5);
    }
    if (mode === 'controls') for (const el of all) {
      if (el.matches(':disabled') || el.closest(':disabled')) continue;
      const s = getComputedStyle(el), outside = background(el.parentElement), inside = background(el);
      if (el.matches('input,select,textarea,button') && (el.matches('input,select,textarea,[aria-pressed]') || parseFloat(s.borderTopWidth) > 0)) {
        const border = over(parse(s.borderTopColor), inside);
        // A filled control may establish its boundary through its fill instead.
        check(el, 'boundary', Math.max(ratio(border, outside), ratio(inside, outside)), 3);
      }
      if (el.matches('[aria-current="page"],.booking-tabs .active,.day-header.today,[aria-pressed="true"]')) {
        const borderRatios = ['Top', 'Right', 'Bottom', 'Left'].map(side => {
          const style = s as unknown as Record<string, string>;
          return parseFloat(style[`border${side}Width`]) > 0 ? ratio(over(parse(style[`border${side}Color`]), inside), outside) : 1;
        });
        check(el, 'selected indicator', Math.max(ratio(inside, outside), ...borderRatios), 3);
      }
    }
    if (mode === 'focus') {
      const el = document.activeElement as HTMLElement;
      if (!el || el === document.body || el.matches(':disabled')) return { checked: 0, failures: [] };
      const s = getComputedStyle(el); const adjacent = background(el.parentElement);
      const outline = s.outlineStyle !== 'none' && parseFloat(s.outlineWidth) >= 1 ? ratio(over(parse(s.outlineColor), adjacent), adjacent) : 1;
      const shadowColors = s.boxShadow.match(/rgba?\([^)]+\)/g) ?? [];
      const shadow = shadowColors.map(c => ratio(over(parse(c), adjacent), adjacent));
      check(el, 'keyboard focus indicator', Math.max(outline, ...shadow), 3);
    }
    return { checked, failures };
  }, mode);
}

