import { isObject } from 'rapidcontext/fn';
import { object, clone, map, filter, sort, uniq } from 'rapidcontext/data';
import { startCase } from 'rapidcontext/text';
import { create, msg } from 'rapidcontext/ui';
import { typeIds, typePath, objectProps, renderProp, approxSize, approxDuration } from './util.mjs';

const placeholders = {
    connection: 'Enter connection identifier',
    data: 'Enter value or code',
    procedure: 'Enter procedure identifier',
    argument: 'Enter argument description or example',
};

let procedures = [];
const defaults = {};

export default function init(ui) {
    defaults.operatorId = defaults.userId = {
        type: 'string',
        value: RapidContext.App.user().id
    };
    ui.root.on('settings-proc-select', async (evt) => {
        // FIXME: Create vertical tabs component to simplify this?
        const tab = ui.tabs.querySelector('[data-view="procPane"]');
        RapidContext.UI.Event.emit(tab, 'click', { bubbles: true });
        ui.procTree.selectedChild()?.unselect();
        await refresh(ui);
        ui.procTree.findByPath(nodePath(evt.detail))?.select();
    });
    ui.procPane.once('enter', () => refresh(ui));
    ui.procSearch.on('reload', () => refresh(ui));
    ui.procSearch.on('search', () => search(ui));
    ui.procAdd.on('click', () => edit(ui, { type: 'procedure/javascript', bindings: [] }));
    ui.procMetrics.on('click', () => ui.procMetricsDialog.fetch('procedure'));
    ui.procTree.on('select', () => show(ui));
    ui.procDetails.on('click', '[data-action="edit"]', (evt) => {
        callArgEdit(ui, evt.delegateTarget.closest('td').querySelector('input'));
    });
    ui.procCall.on('click', () => call(ui));
    ui.procBatch.on('click', () => batch(ui));
    ui.procEdit.on('click', () => edit(ui, ui.procEditForm.original));
    ui.procDelete.on('click', () => remove(ui));
    ui.procArgForm.on('submit', () => callArgSave(ui));
    ui.procResultForm.on('change', () => {
        const data = ui.procResultForm.valueMap();
        ui.procResultTree.setAttrs({ hidden: data.format != 'tree' });
        ui.procResultTable.setAttrs({ hidden: data.format != 'table' });
        ui.procResultRaw.setAttrs({ hidden: data.format != 'raw' });
        ui.procResultTrace.setAttrs({ hidden: data.format != 'trace' });
    });
    ui.procResultTree.on('expand', (evt) => renderDataTree(evt.detail.node, evt.detail.node.data));
    ui.procBatchDownload.on('click', () => batchDownload(ui));
    ui.procBatchCancel.on('click', () => ui.procBatchForm.cancelled.value = 'true');
    ui.procEditType.addEventListener('change', () => editRefresh(ui));
    ui.procEditForm.on('click', '[data-action]', (evt) => editAction(ui, evt.delegateTarget));
    ui.procEditForm.on('submit', (evt) => save(ui, evt));
}

async function refresh(ui) {
    ui.procSearch.loading = true;
    try {
        procedures = await RapidContext.App.callProc('system/procedure/list', []);
        search(ui);
    } catch (e) {
        RapidContext.UI.showError(e);
    }
    ui.procSearch.loading = false;
}

function search(ui) {
    const q = ui.procSearch.query;
    const data = procedures.filter((p) => {
        return p.toLowerCase().includes(q);
    });
    ui.procSearch.info(data.length, procedures.length, 'procedures');
    ui.procTree.markAll();
    for (const id of data) {
        const node = ui.procTree.addPath(nodePath(id));
        node.data = id;
    }
    ui.procTree.removeAllMarked();
    show(ui);
}

async function show(ui) {
    const id = ui.procTree.selectedChild()?.data;
    if (id) {
        try {
            ui.procIdLink.value = id;
            ui.procPropTpl.clear();
            ui.procArgTpl.clear();
            ui.procDetails.show();
            const data = await RapidContext.App.callProc('system/procedure/read', [id]);
            const ignore = ['id', 'name', 'className', 'binding', 'bindings', 'local'];
            const props = objectProps(data, ignore).map((p) => renderProp(p, data));
            ui.procPropTpl.render(props);
            const args = data.bindings.filter((b) => b.type == 'argument').map((b, idx) => {
                const title = startCase(b.name);
                const name = b.name;
                const value = '';
                const placeholder = b.value;
                const help = b.description;
                return { title, name, value, placeholder, help };
            });
            ui.procArgTpl.render(args);
            for (const a of callArgs(ui)) {
                if (a.name in defaults) {
                    callArgSet(a.input, defaults[a.name]);
                }
            }
            ui.procCall.show();
            ui.procBatch.hide();
            ui.procEdit.setAttrs({ hidden: data.type == 'procedure' });
            ui.procEditForm.original = filter((v, k) => !['name', 'local'].includes(k), data);
            ui.procDelete.setAttrs({ hidden: !data.local });
        } catch (e) {
            RapidContext.UI.showError(e);
        }
    } else {
        ui.procDetails.hide();
    }
}

function callArgs(ui) {
    const inputs = Array.from(ui.procDetails.elements).filter((el) => el.name);
    return inputs.map((el) => {
        const name = el.name;
        const raw = el.dataset.value ?? el.value;
        const type = el.dataset.type ?? 'string';
        const value = (type === 'json' || type === 'array') ? JSON.parse(raw) : raw;
        return { name, value, type, raw, input: el };
    });
}

function callArgSet(input, data) {
    if (data.type === 'json') {
        input.value = JSON.stringify(JSON.parse(data.value));
        input.disabled = true;
        input.dataset.type = 'json';
        input.dataset.value = data.value;
    } else if (data.type === 'array') {
        const arr = data.value.startsWith('[') ? JSON.parse(data.value) : data.value.trim().split('\n');
        input.value = `Batch: ${arr.length} entries`;
        input.disabled = true;
        input.dataset.type = 'array';
        input.dataset.value = JSON.stringify(arr);
    } else if (data.type === 'string' && !data.value.includes('\n')) {
        input.value = data.value;
        input.disabled = false;
        delete input.dataset.type;
        delete input.dataset.value;
    } else {
        input.value = data.value.trim().split('\n').join(' \u23ce ');
        input.disabled = true;
        input.dataset.type = data.type;
        input.dataset.value = data.value;
    }
}

function callArgEdit(ui, input) {
    const name = input.name;
    const raw = input.dataset.value ?? input.value;
    const type = input.dataset.type ?? 'string';
    const value = (type == 'array') ? JSON.parse(raw).join('\n') : raw;
    ui.procArgForm.reset();
    ui.procArgForm.update({ name, type, value });
    ui.procArgDialog.show();
    ui.procArgForm.querySelector('textarea').focus();
}

function callArgSave(ui) {
    ui.procArgValueValidator.reset();
    try {
        const data = ui.procArgForm.valueMap();
        const input = ui.procDetails.elements[data.name];
        callArgSet(input, data);
        ui.procArgDialog.hide();
        const isBatch = callArgs(ui).some((a) => a.type == 'array');
        ui.procCall.setAttrs({ hidden: isBatch });
        ui.procBatch.setAttrs({ hidden: !isBatch });
    } catch (e) {
        ui.procArgValueValidator.addError(ui.procArgForm.elements.value, `Invalid JSON: ${e.message}`);
    }
}

async function call(ui) {
    function callProc(id) {
        const params = { 'system:trace': 'true' };
        for (const a of callArgs(ui)) {
            if (a.value == null || a.value == '') {
                delete defaults[a.name];
            } else {
                defaults[a.name] = { type: a.type, value: a.raw };
            }
            params[a.name] = (a.type === 'json') ? a.raw : RapidContext.Encode.toJSON(a.value);
            if (a.raw == '' && a.input.placeholder) {
                delete params[a.name]; // exclude empty args with binding value
            }
        }
        const opts = { method: 'POST', timeout: 60000, responseType: 'text' };
        return RapidContext.App.loadXHR(`rapidcontext/procedure/${id}`, params, opts);
    }
    try {
        const id = ui.procTree.selectedChild()?.data;
        ui.procResultForm.reset();
        ui.procResultForm.update({ id });
        ui.procResultLoading.show();
        ui.procResultTree.removeAll();
        ui.procResultTable.removeAll();
        ui.procResultDialog.show();
        const promise = callProc(id);
        ui.procResultDialog.on('hide', () => promise.cancel());
        const xhr = await promise;
        const json = JSON.parse(xhr.response);
        const size = approxSize(+xhr.getResponseHeader('Content-Length'));
        const duration = approxDuration(json.execTime);
        const text = JSON.stringify(json.data, null, 2);
        const trace = json.trace ?? '';
        ui.procResultForm.update({ size, duration, text, trace });
        if (json.error) {
            ui.procResultForm.update({ format: 'raw', text: `Error: ${json.error}` });
        } else {
            renderDataTree(ui.procResultTree, json.data);
            renderDataTable(ui.procResultTable, json.data);
        }
    } catch (e) {
        ui.procResultForm.update({ format: 'raw', text: String(e) });
    }
    ui.procResultDialog.off('hide');
    ui.procResultLoading.hide();
}

async function batch(ui) {
    function toParams(args) {
        const val = {};
        const arr = {};
        let count = null;
        for (const a of args) {
            val[a.name] = a.value;
            arr[a.name] = a.type === 'array';
            if (arr[a.name] && !count) {
                count = a.value.length;
            } else if (arr[a.name] && count !== a.value.length) {
                throw new Error(`mismatching arguments for "${a.name}": expected ${count} values`);
            }
            if (a.raw == '' && a.input.placeholder) {
                delete val[a.name]; // exclude empty args with binding value
            }
        }
        const params = [];
        for (let i = 0; i < count; i++) {
            params.push(map((v, k) => arr[k] ? v[i] : v, val));
        }
        return params;
    }
    async function call(id, args) {
        if (!ui.procBatchForm.cancelled.value) {
            try {
                const res = await RapidContext.App.callProc(id, args);
                store(args, res);
            } catch (e) {
                store(args, { error: e.message });
            }
        }
        if (!ui.procBatchForm.cancelled.value) {
            const delay = +ui.procBatchForm.delay.value.trim() || 0.1;
            await RapidContext.Async.wait(delay * 1000);
        }
    }
    function store(args, res) {
        const row = { ...args };
        if (isObject(res)) {
            for (const k in res) {
                row[k] = (k in row) ? row[k] : res[k];
            }
        }
        row.result = JSON.stringify(res);
        const results = ui.procBatchTable.results;
        results.push(row);
        if (results.length == 1) {
            renderDataTable(ui.procBatchTable, results);
        } else if (results.length <= 1000) {
            ui.procBatchTable.setData(results);
        }
    }
    try {
        const id = ui.procTree.selectedChild()?.data;
        const params = toParams(callArgs(ui));
        ui.procBatchForm.reset();
        ui.procBatchForm.update({ id, cancelled: '' });
        ui.procBatchProgress.setAttrs({ min: 0, max: params.length, value: 0 });
        ui.procBatchTable.removeAll();
        ui.procBatchTable.results = [];
        ui.procBatchCancel.show();
        ui.procBatchClose.hide();
        ui.procBatchDialog.show();
        for (let i = 0; i < params.length; i++) {
            ui.procBatchProgress.setAttrs({ value: i + 1 });
            await call(id, params[i]);
        }
        ui.procBatchCancel.hide();
        ui.procBatchClose.show();
    } catch (e) {
        RapidContext.UI.showError(e);
    }
}

function batchDownload(ui) {
    function repr(val) {
        const isObj = val == null || Array.isArray(val) || isObject(val);
        return isObj ? JSON.stringify(val) : String(val);
    }
    function encode(val) {
        const s = repr(val).replaceAll(/"/g, '""');
        return /[,"\r\n]/.test(s) ? `"${s}"` : s;
    }
    function csv(props, rows) {
        return [
            props.map(encode).join(','),
            ...rows.map((o) => props.map((p) => encode(o[p])).join(','))
        ].join('\r\n');
    }
    function dowloadFile(filename, buffer) {
        const blob = new Blob([buffer]);
        const href = URL.createObjectURL(blob);
        setTimeout(URL.revokeObjectURL.bind(URL, href), 60 * 1000);
        const a = document.createElement('a');
        a.setAttribute('href', href);
        a.setAttribute('download', filename);
        a.dispatchEvent(new MouseEvent('click'));
    }
    const id = ui.procTree.selectedChild()?.data;
    const now = new Date().toISOString().replace(/T(\d+):(\d+):.*/, '-$1$2');
    const results = ui.procBatchTable.results;
    let props = [];
    for (let i = 0; i < Math.min(10, results.length); i++) {
        props = uniq([...props, ...Object.keys(results[i])]);
    }
    dowloadFile(`batch-${id.replace(/[^a-z0-9_-]/gi, '-')}-${now}.csv`, csv(props, results));
}

function edit(ui, data) {
    const createOption = (id) => create('option', { value: id }, id.replace('procedure/', ''));
    ui.procEditType.replaceChildren();
    ui.procEditType.append(...typeIds('procedure').map(createOption));
    ui.procEditForm.reset();
    ui.procEditForm.update(data);
    ui.procEditForm.original = data;
    ui.procEditForm.data = clone(data);
    ui.procEditDialog.show();
    editRender(ui); // Render initial form
    editRefresh(ui); // Re-render including type bindings
}

function editRender(ui) {
    function buildBtn(icon, action) {
        return RapidContext.Widget.Button({
            icon,
            'class': 'font-smaller ml-1',
            'data-action': action,
        });
    }
    function buildRow(tpl, b, def) {
        const isArg = b.type == 'argument';
        const help = def ? def.description : (!isArg && b.description) || '';
        const input = RapidContext.Widget.TextArea({
            name: `binding.${b.name}`,
            value: isArg ? b.description : b.value,
            placeholder: placeholders[b.type],
            autosize: true,
            rows: 1,
            wrap: 'off',
            'class': 'flex-fill',
        });
        const up = def ? '' : buildBtn('fa fa-lg fa-level-down', 'down');
        const rm = def ? '' : buildBtn('fa fa-lg fa-minus', 'remove');
        const tr = tpl.render({ name: b.name, help })[0];
        tr.lastChild.firstChild.append(input, up, rm);
        return tr;
    }
    const data = ui.procEditForm.data;
    const type = typePath(data.type).pop();
    ui.procEditTypeHelp.replaceChildren(type?.description ?? '');
    const tpls = {
        connection: ui.procEditConTpl,
        data: ui.procEditDataTpl,
        procedure: ui.procEditProcTpl,
        argument: ui.procEditArgTpl
    };
    for (const type in tpls) {
        tpls[type].clear();
    }
    const defs = object('name', type?.binding ?? []);
    data.bindings.forEach((b) => buildRow(tpls[b.type], b, defs[b.name]));
    for (const type in tpls) {
        const el = tpls[type].closest('fieldset');
        el.classList.toggle('hidden', tpls[type].copies().length == 0);
    }
    const isTypeJs = data.type == 'procedure/javascript';
    ui.procEditAddBinding.querySelectorAll('option').forEach((el, idx) => {
        if (el.value != 'argument') {
            el.disabled = !isTypeJs;
        } else if (!isTypeJs) {
            ui.procEditAddBinding.selectedIndex = idx;
        }
    });
    ui.procEditForm.update(data);
    ui.procEditDialog.moveToCenter();
}

function editRefresh(ui) {
    const typeOrder = {
        connection: 1,
        data: 2,
        procedure: 3,
        argument: 4,
    };
    const data = ui.procEditForm.data;
    const values = ui.procEditForm.valueMap();
    const type = typePath(values.type).pop();
    const bindings = object('name', type.binding ?? []);
    for (const b of data.bindings) {
        const key = (b.type == 'argument') ? 'description' : 'value';
        const val = (values[`binding.${b.name}`] ?? '').trim();
        // Exclude default bindings from old procedure type
        if (val || !b.description) {
            bindings[b.name] = {
                ...bindings[b.name],
                name: b.name,
                type: b.type,
                [key]: val,
            };
            if (b.type == 'argument' && b.value != null && b.value != '') {
                bindings[b.name].value = b.value;
            }
        }
    }
    data.id = values.id;
    data.type = values.type;
    data.description = values.description;
    data.bindings = sort((b) => typeOrder[b.type], bindings);
    editRender(ui);
}

function editAction(ui, el) {
    const data = ui.procEditForm.data;
    if (el.dataset.action === 'add') {
        const type = ui.procEditAddBinding.value;
        const name = ui.procEditAddName.value.trim();
        ui.procEditAddValidator1.reset();
        ui.procEditAddValidator2.reset();
        if (!/^[a-z0-9_-]+$/i.test(name)) {
            ui.procEditAddValidator1.addError(ui.procEditAddName);
        } else if (data.bindings.find((b) => b.name == name)) {
            ui.procEditAddValidator2.addError(ui.procEditAddName);
        } else {
            ui.procEditAddName.setAttrs({ value: '' });
            const key = (type == 'argument') ? 'description' : 'value';
            data.bindings.push({ name, type, [key]: '' });
            editRefresh(ui);
            ui.procEditForm.querySelector(`textarea[name="binding.${name}"]`).focus();
        }
    } else if (el.dataset.action === 'remove') {
        const cur = el.closest('[data-binding]');
        data.bindings = data.bindings.filter((b) => b.name != cur.dataset.binding);
        cur.remove();
        editRefresh(ui);
    } else if (el.dataset.action === 'down') {
        const cur = el.closest('[data-binding]');
        const next = cur.nextElementSibling;
        if (next?.matches('[data-binding]')) {
            const ix1 = data.bindings.findIndex((b) => b.name == cur.dataset.binding);
            const ix2 = data.bindings.findIndex((b) => b.name == next.dataset.binding);
            swap(data.bindings, ix1, ix2);
            cur.parentNode.insertBefore(next, cur);
            cur.querySelector('textarea').focus();
        }
    }
}

async function save(ui, evt) {
    evt.preventDefault();
    editRefresh(ui);
    ui.procEditForm.querySelectorAll('button').forEach((el) => el.disabled = true);
    try {
        const orig = ui.procEditForm.original;
        const data = ui.procEditForm.data;
        const oldPath = orig.id ? `/procedure/${orig.id}` : null;
        const newPath = `/procedure/${data.id}`;
        data.binding = data.bindings.map((b) => {
            const res = { name: b.name, type: b.type, value: b.value };
            if (b.type == 'argument') {
                (res.value == null) && delete res.value;
                res.description = b.description;
            }
            return res;
        });
        delete data.bindings;
        let opts = { path: `${newPath }.yaml` };
        if (oldPath && oldPath !== newPath) {
            opts = { path: oldPath, updateTo: opts.path };
        } else if (oldPath) {
            opts.update = true;
        }
        await RapidContext.App.callProc('system/storage/write', [opts, data]);
        ui.procEditDialog.hide();
        ui.procTree.selectedChild()?.unselect();
        await refresh(ui);
        ui.procTree.findByPath(nodePath(data.id))?.select();
    } catch (e) {
        RapidContext.UI.showError(e);
    } finally {
        ui.procEditForm.querySelectorAll('button').forEach((el) => el.disabled = false);
    }
}

async function remove(ui) {
    try {
        const id = ui.procTree.selectedChild()?.data;
        await msg.warning.remove('procedure', id);
        await RapidContext.App.callProc('system/storage/delete', [`/procedure/${id}`]);
        await refresh(ui);
    } catch (e) {
        // FIXME: Better detection of cancelled operation?
        if (e.message != 'operation cancelled') {
            RapidContext.UI.showError(e);
        }
    }
}

function renderDataTree(node, data) {
    function typeName(val) {
        if (val == null) {
            return 'null';
        } else if (typeof(val) == 'object') {
            const s = Object.prototype.toString.call(val);
            return s.replace('[object ', '').replace(']', '');
        } else {
            return typeof(val);
        }
    }
    function label(val) {
        if (val == null) {
            return '<null>';
        } else if (Array.isArray(val)) {
            return `[Array; length: ${val.length}]`;
        } else if (isObject(val)) {
            return `[Object; size: ${Object.keys(val).length}]`;
        } else {
            const lines = String(val).split('\n');
            const suffix = (lines.length > 1) ? '\u2026' : '';
            return `[${typeName(val)}] ${lines[0]}${suffix}`;
        }
    }
    if (node.getChildNodes().length > 0) {
        // Do nothing
    } else if (Array.isArray(data) || isObject(data)) {
        for (const k in data) {
            const v = /^\d+$/.test(k) ? data[+k] : data[k];
            const name = `${k}: ${label(v)}`;
            const folder = Array.isArray(v) || isObject(v);
            const extra = folder ? {} : { tooltip: String(v) };
            const child = RapidContext.Widget.TreeNode({ name, folder, ...extra });
            child.data = v;
            node.addAll(child);
        }
    } else {
        const name = label(data);
        const tooltip = String(data);
        const child = RapidContext.Widget.TreeNode({ name, tooltip });
        node.addAll(child);
    }
}

function renderDataTable(table, data) {
    if (isObject(data)) {
        data = Object.keys(data).map((k) => {
            const v = data[k];
            return isObject(v) ? { key: k, ...v } : { key: k, value: v };
        });
    } else if (!Array.isArray(data)) {
        data = [data].filter(Boolean);
    }
    const arr = data.slice(0, 1000).map((o) => isObject(o) ? o : { value: o });
    const props = Object.keys(arr[0] ?? {}).slice(0, 7);
    const toCol = (k) => ({
        field: k,
        title: startCase(k),
        maxLength: 40,
        cellStyle: 'text-nowrap'
    });
    table.removeAll();
    table.addAll(props.map((k) => RapidContext.Widget.TableColumn(toCol(k))));
    table.setData(arr);
}

function nodePath(id) {
    return id.split(/[./]/g);
}

function swap(arr, ix1, ix2) {
    const el = arr[ix1];
    arr[ix1] = arr[ix2];
    arr[ix2] = el;
}
