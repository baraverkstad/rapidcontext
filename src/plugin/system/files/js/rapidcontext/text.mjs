export function str(val) {
    return (val === null || val === undefined) ? '' : String(val);
}

export function lower(val) {
    return str(val).toLowerCase();
}

export function upper(val) {
    return str(val).toUpperCase();
}

export function upperFirst(val) {
    let s = str(val);
    return s.charAt(0).toUpperCase() + s.slice(1);
}

export function titleCase(val) {
    let s = str(val).replace(/_/g, ' ').trim();
    return s.toLowerCase().split(/\s+/g).map(upperFirst).join(' ');
}

export function pluralize(val, suffix, plural) {
    plural = plural || (suffix && suffix + 's') || '';
    return [val, val === 1 ? (suffix || '') : plural].join(' ').trim();
}

export default { str, lower, upper, upperFirst, titleCase, pluralize };
