import { hasValue, isObject } from 'rapidcontext/fn';
import { object, flatten, sort } from 'rapidcontext/data';

let textarea = document.createElement('textarea');
let types = {};

export function str(val) {
    return (val == null) ? '' : String(val);
}

export function text(val) {
    let lines = str(val).split(/\n\n+/g);
    if (lines.length > 1) {
        return lines.map((s) => s.replace(/\s+/g, ' ')).join('\n\n');
    } else {
        return lines[0];
    }
}

export function escape(val) {
    textarea.innerText = (val == null) ? '' : [].concat(val).join('');
    return textarea.innerHTML.replace(/'/g, '&apos;').replace(/"/g, '&quot;');
}

export function template(tpl, data) {
    if (tpl instanceof Element) {
        let html = template(tpl.outerHTML, data);
        tpl.insertAdjacentHTML('beforebegin', html);
        return tpl.previousSibling;
    } else {
        tpl = String(tpl);
        let wrap = tpl.startsWith('<') && tpl.endsWith('>') ? escape : str;
        return tpl.replace(/{{([^}]+)}}/g, (_, id) => wrap(data && data[id]) || '');
    }
}

export async function loadTypes() {
    let data = await RapidContext.App.callProc('system/storage/read', ['/type/']);
    return types = object('id', data);
}

export function typeIds(base) {
    return Object.keys(types).filter((id) => id.startsWith(base + '/'));
}

export function typePath(type) {
    let path = type.split('/');
    return path.map((_, idx) => {
        let id = path.slice(0, idx + 1).join('/');
        return types[id];
    }).filter(Boolean);
}

function isUnseen(key, seen) {
    key = key.startsWith('_') ? key.substring(1) : key;
    let found = seen.includes(key) || seen.includes(`_${key}`);
    !found && seen.push(key, `_${key}`);
    return !found;
}

export function typeProps(type, ignore) {
    ignore = ['_activatedTime'].concat(ignore);
    let typeDefs = [types['object'], ...typePath(type)];
    let props = flatten(typeDefs.map((o) => o && o.property || []));
    return props.filter((p) => /^\.?[a-z0-9_-]+$/i.test(p.name) && isUnseen(p.name, ignore));
}

export function objectProps(data, ignore) {
    function create(key) {
        return { name: key, required: false, format: 'text', custom: true };
    }
    ignore = ['_activatedTime'].concat(ignore);
    let keys = Object.keys(data).filter((k) => hasValue(data[k]));
    let props = typeProps(data.type, []).concat(keys.map(create));
    let sortBy = (p) => +p.name.startsWith('_') * 10 + +!p.required;
    return sort(sortBy, props).filter((p) => isUnseen(p.name, ignore));
}

export function renderProp(prop, data) {
    let isSet = hasValue(data[prop.name]);
    let isSetOrDefault = isSet || hasValue(data[`_${prop.name}`]);
    let title = prop.title || RapidContext.Util.toTitleCase(prop.name);
    let value = isSet ? data[prop.name] : data[`_${prop.name}`];
    if (Array.isArray(value)) {
        value = value.join(' \u2022 ');
    } else if (isObject(value)) {
        value = JSON.stringify(value, null, 2);
    } else if (/^@\d+$/.test(value)) {
        let dt = new Date(+value.substr(1));
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
    let factor = Math.pow(10, digits);
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
    } else if (millis > 1000) {
        return `${round(millis / 1000, 1)} s`;
    } else {
        return `${millis} ms`;
    }
}
