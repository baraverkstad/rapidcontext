/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

/**
 * Creates a DOM element with attributes and child content. Also supports
 * building a UI recursively from either an XML document or an XML string.
 *
 * @param {String/Node} nodeOrName the element name or UI to create
 * @param {Object} [attrs] the optional name-value attribute mappings
 * @param {Node/String} [...content] the child nodes or text
 *
 * @return {Node} the DOM element node created
 *
 * @function RapidContext.UI.create
 */
export default function create(nodeOrName, attrs, ...content) {
    if (RapidContext.Widget.Classes[nodeOrName]) {
        return RapidContext.Widget.Classes[nodeOrName](attrs, ...content);
    } else if (/^[a-z0-9_-]+$/i.test(nodeOrName)) {
        let el = document.createElement(nodeOrName);
        for (let k in (attrs || {})) {
            el.setAttribute(k, attrs[k]);
        }
        el.append(...content.filter(isNotNull));
        return el;
    } else if (/^<[\s\S]+>$/.test(nodeOrName)) {
        let node = new DOMParser().parseFromString(nodeOrName, 'text/xml');
        return create(node.documentElement);
    } else if (nodeOrName.nodeType === 1) { // Node.ELEMENT_NODE
        return createElem(nodeOrName);
    } else if (nodeOrName.nodeType === 3) { // Node.TEXT_NODE
        let str = nodeOrName.nodeValue || '';
        return str.trim() ? document.createTextNode(str) : null;
    } else if (nodeOrName.nodeType === 4) { // Node.CDATA_SECTION_NODE
        let str = nodeOrName.nodeValue || '';
        return str ? document.createTextNode(str) : null;
    } else if (nodeOrName.nodeType === 9) { // Node.DOCUMENT_NODE
        return create(nodeOrName.documentElement);
    } else {
        return null;
    }
}

// Creates a DOM element from a UI XML node
function createElem(node) {
    let name = node.nodeName;
    if (name == 'style') {
        document.head.append(createStyleElem(node.innerText));
        node.parentNode.removeChild(node);
        return null;
    } else if (name == 'script') {
        console.warn('script injection is unsupported in UI XML', node);
        return null;
    }
    let attrs = Array.from(node.attributes).reduce((o, a) => ({ ...o, [a.name]: a.value }), {});
    let children = Array.from(node.childNodes).map((o) => create(o)).filter(Boolean);
    let el = create(name, attrs, ...children);
    if ('id' in attrs) {
        el.setAttribute('id', attrs.id);
    }
    if ('w' in attrs) {
        el.style.width = toCssLength(attrs.w);
    }
    if ('h' in attrs) {
        el.style.height = toCssLength(attrs.h);
    }
    return el;
}

// Creates a DOM style element with specified CSS rules
function createStyleElem(css) {
    let style = document.createElement('style');
    style.setAttribute('type', 'text/css');
    try {
        style.innerHTML = css;
    } catch (e) {
        let parts = css.split(/\s*[{}]\s*/);
        for (let i = 0; i < parts.length; i += 2) {
            let rules = parts[i].split(/\s*,\s*/);
            let styles = parts[i + 1];
            for (let j = 0; j < rules.length; j++) {
                let rule = rules[j].replace(/\s+/, ' ').trim();
                style.styleSheet.addRule(rule, styles);
            }
        }
    }
}

// Translates a short length into a CSS calc() expression
function toCssLength(val) {
    if (/[+-]/.test(val)) {
        val = 'calc( ' + val.replace(/[+-]/g, ' $& ') + ' )';
    }
    val = val.replace(/(\d)( |$)/g, '$1px$2');
    return val;
}

function isNotNull(o) {
    return o != null;
}
