/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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

import data from './data.mjs';
import fn from './fn.mjs';
import ui from './ui/index.mjs';

export { data, fn, ui };
export default { data, fn, ui };

// Note: Setting window.RapidContext here for legacy global API
let RapidContext = globalThis.RapidContext || (globalThis.RapidContext = {});
Object.assign(RapidContext, { Data: data, Fn: fn, UI: RapidContext.UI || {} });
Object.assign(RapidContext.UI, { create: ui.create, Event: ui.event, Msg: ui.msg });
[
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hr', 'p', 'pre', 'div',
    'b', 'i', 'em', 'small', 'strong', 'code', 'var', 'span', 'br',
    'a', 'img', 'picture', 'video', 'source', 'object',
    'ol', 'ul', 'li', 'dl', 'dd', 'dt',
    'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td',
    'dialog', 'menu', 'meter', 'nav', 'output', 'progress',
    'form', 'label', 'button', 'input', 'textarea', 'select',
    'option', 'optgroup', 'fieldset', 'datalist',
].forEach((k) => RapidContext.UI[k.toUpperCase()] = ui.create.bind(null, k));
