import { hasValue, isObject } from 'rapidcontext/fn';
import { object, flatten, sort } from 'rapidcontext/data';

const textarea = document.createElement('textarea');
let types = {};

export function str(val) {
    return (val == null) ? '' : String(val);
}

export function text(val) {
    const lines = str(val).split(/\n\n+/g);
    if (lines.length > 1) {
        return lines.map((s) => s.replaceAll(/\s+/g, ' ')).join('\n\n');
    } else {
        return lines[0];
    }
}

export function escape(val) {
    textarea.innerText = (val == null) ? '' : [].concat(val).join('');
    return textarea.innerHTML.replaceAll(/'/g, '&apos;').replaceAll(/"/g, '&quot;');
}

export function template(tpl, data) {
    if (tpl instanceof Element) {
        const html = template(tpl.outerHTML, data);
        tpl.insertAdjacentHTML('beforebegin', html);
        return tpl.previousSibling;
    } else {
        tpl = String(tpl);
        const wrap = tpl.startsWith('<') && tpl.endsWith('>') ? escape : str;
        return tpl.replaceAll(/{{([^}]+)}}/g, (_, id) => wrap(data && data[id]) || '');
    }
}

export async function loadTypes() {
    const data = await RapidContext.App.callProc('system/storage/read', ['/type/']);
    return types = object('id', data);
}

export function typeIds(base) {
    return Object.keys(types).filter((id) => id.startsWith(`${base}/`));
}

export function typePath(type) {
    const path = type.split('/');
    return path.map((_, idx) => {
        const id = path.slice(0, idx + 1).join('/');
        return types[id];
    }).filter(Boolean);
}

function isUnseen(key, seen) {
    key = key.startsWith('_') ? key.substring(1) : key;
    const found = seen.includes(key) || seen.includes(`_${key}`);
    !found && seen.push(key, `_${key}`);
    return !found;
}

export function typeProps(type, ignore) {
    ignore = ['_activatedTime'].concat(ignore);
    const typeDefs = [types['object'], ...typePath(type)];
    const props = flatten(typeDefs.map((o) => o && o.property || []));
    return props.filter((p) => /^\.?[a-z0-9_-]+$/i.test(p.name) && isUnseen(p.name, ignore));
}

export function objectProps(data, ignore) {
    function create(key) {
        return { name: key, required: false, format: 'text', custom: true };
    }
    ignore = ['_activatedTime'].concat(ignore);
    const keys = Object.keys(data).filter((k) => hasValue(data[k]));
    const props = typeProps(data.type, []).concat(keys.map(create));
    const sortBy = (p) => +p.name.startsWith('_') * 10 + +!p.required;
    return sort(sortBy, props).filter((p) => isUnseen(p.name, ignore));
}

export function renderProp(prop, data) {
    const isSet = hasValue(data[prop.name]);
    const isSetOrDefault = isSet || hasValue(data[`_${prop.name}`]);
    const title = prop.title || RapidContext.Util.toTitleCase(prop.name);
    let value = isSet ? data[prop.name] : data[`_${prop.name}`];
    if (Array.isArray(value)) {
        value = value.join(' \u2022 ');
    } else if (isObject(value)) {
        value = JSON.stringify(value, null, 2);
    } else if (/^@\d+$/.test(value)) {
        const dt = new Date(+value.substr(1));
        value = [dt.toISOString().replace('T', ' ').replace(/\.\d+Z/, ''), value].join(' \u2022 ');
    } else {
        value = text(value) || '\u2014';
    }
    let style = '';
    if (!isSetOrDefault && !prop.required) {
        style = 'hidden';
    } else if (isSetOrDefault && prop.format == 'error') {
        style = 'color-danger';
    } else if (!isSet || prop.name.startsWith('_')) {
        style = 'default-value';
    }
    return { title, value, style };
}

export function round(num, digits) {
    const factor = Math.pow(10, digits);
    return Math.round((num + Number.EPSILON) * factor) / factor;
}

export function approxSize(bytes) {
    if (isNaN(bytes)) {
        return '\u2014';
    } else if (bytes > 1000000) {
        return `${round(bytes / 1048576, 1)} MiB`;
    } else if (bytes > 2000) {
        return `${round(bytes / 1024, 1)} KiB`;
    } else {
        return `${bytes} bytes`;
    }
}

export function approxDuration(millis) {
    if (isNaN(millis)) {
        return '\u2014';
    } else if (millis > 129600000) {
        return `${round(millis / 86400000, 1)} days`;
    } else if (millis > 5400000) {
        return `${round(millis / 3600000, 1)} hours`;
    } else if (millis > 90000) {
        return `${round(millis / 60000, 1)} mins`;
    } else if (millis > 1000) {
        return `${round(millis / 1000, 1)} secs`;
    } else {
        return `${millis} ms`;
    }
}
