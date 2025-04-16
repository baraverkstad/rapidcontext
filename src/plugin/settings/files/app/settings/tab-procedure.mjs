import { isObject } from 'rapidcontext/fn';
import { object, clone } from 'rapidcontext/data';
import { create, msg } from 'rapidcontext/ui';
import { typeIds, typePath, objectProps, renderProp, approxSize, approxDuration } from './util.mjs';

/* eslint require-await: "off" */

const placeholders = {
    connection: 'Enter connection identifier',
    data: 'Enter value or code',
    procedure: 'Enter procedure identifier',
    argument: 'Enter argument description or example',
};

let procedures = [];
let defaults = {};
let active = null;

export default async function init(ui) {
    defaults.operatorId = defaults.userId = {
        type: 'string',
        value: RapidContext.App.user().id
    };
    ui.procPane.once('enter', () => refresh(ui));
    ui.procSearch.on('reload', () => refresh(ui));
    ui.procSearch.on('search', () => search(ui));
    ui.procAdd.on('click', () => edit(ui, { type: 'procedure/javascript', bindings: [] }));
    ui.procTree.on('select', () => show(ui));
    ui.procDetails.on('click', '[data-action="edit"]', (evt) => {
        callArgEdit(ui, evt.delegateTarget.closest('td').querySelector('input'));
    });
    ui.procCall.on('click', () => call(ui));
    ui.procEdit.on('click', () => edit(ui, active));
    ui.procDelete.on('click', () => remove(ui));
    ui.procArgForm.on('submit', () => callArgSave(ui));
    ui.procResultForm.on('change', () => {
        let data = ui.procResultForm.valueMap();
        ui.procResultTree.setAttrs({ hidden: data.format != 'tree' });
        ui.procResultTable.setAttrs({ hidden: data.format != 'table' });
        ui.procResultRaw.setAttrs({ hidden: data.format != 'raw' });
    });
    ui.procResultTree.on('expand', (evt) => renderDataTree(evt.detail.node, evt.detail.node.data));
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
    let q = ui.procSearch.query;
    let data = procedures.filter((p) => {
        return p.toLowerCase().includes(q);
    });
    ui.procSearch.info(data.length, procedures.length, 'procedures');
    ui.procTree.markAll();
    for (let id of data) {
        let node = ui.procTree.addPath(id.split(/[./]/g));
        node.data = id;
    }
    ui.procTree.removeAllMarked();
    show(ui);
}

async function show(ui) {
    let node = ui.procTree.selectedChild();
    if (node && node.data) {
        try {
            ui.procIdLink.value = node.data;
            ui.procPropTpl.clear();
            ui.procArgTpl.clear();
            ui.procDetails.show();
            let data = active = await RapidContext.App.callProc('system/procedure/read', [node.data]);
            let ignore = ['id', 'name', 'className', 'binding', 'bindings', 'local'];
            let props = objectProps(data, ignore).map((p) => renderProp(p, data));
            ui.procPropTpl.render(props);
            let args = data.bindings.filter((b) => b.type == 'argument').map((b, idx) => {
                let binding = b.name;
                let title = RapidContext.Util.toTitleCase(b.name);
                let name = b.name;
                let value = '';
                let help = b.description;
                return { binding, title, name, value, help };
            });
            ui.procArgTpl.render(args);
            ui.procDetails.querySelectorAll('[data-binding]').forEach((el) => {
                if (el.dataset.binding in defaults) {
                    callArgSet(el, defaults[el.dataset.binding]);
                }
            });
            ui.procEdit.setAttrs({ hidden: data.type == 'procedure' });
            ui.procDelete.setAttrs({ hidden: !data.local });
        } catch (e) {
            RapidContext.UI.showError(e);
        }
    } else {
        ui.procDetails.hide();
    }
}

function callArgSet(input, data) {
    if (data.type === 'json') {
        input.value = JSON.stringify(JSON.parse(data.value));
        input.disabled = true;
        input.dataset.type = 'json';
        input.dataset.value = data.value;
    } else if (data.type === 'string' && !data.value.includes('\n')) {
        input.value = data.value;
        input.disabled = false;
        delete input.dataset.type;
        delete input.dataset.value;
    } else {
        input.value = data.value.split('\n').join(' \u23ce ');
        input.disabled = true;
        input.dataset.type = data.type;
        input.dataset.value = data.value;
    }
}

function callArgEdit(ui, input) {
    let name = input.name;
    let type = input.dataset.type || 'string';
    let value = input.dataset.value || input.value;
    ui.procArgForm.reset();
    ui.procArgForm.update({ name, type, value });
    ui.procArgDialog.show();
    ui.procArgForm.querySelector('textarea').focus();
}

function callArgSave(ui) {
    ui.procArgValueValidator.reset();
    try {
        let data = ui.procArgForm.valueMap();
        let input = ui.procDetails.elements[data.name];
        callArgSet(input, data);
        ui.procArgDialog.hide();
    } catch (e) {
        ui.procArgValueValidator.addError(ui.procArgForm.elements.value, `Invalid JSON: ${e.message}`);
    }
}

async function call(ui) {
    function toParam(el) {
        let type = el.dataset.type || 'string';
        let value = el.dataset.value || el.value;
        defaults[el.dataset.binding] = { type, value };
        return [el.name, (type === 'json') ? value : RapidContext.Encode.toJSON(value)];
    }
    function callProc(id) {
        let inputs = Array.from(ui.procDetails.elements).filter((el) => el.name);
        let params = object(inputs.map(toParam));
        let opts = { method: 'POST', timeout: 60000, responseType: 'text' };
        return RapidContext.App.loadXHR(`rapidcontext/procedure/${id}`, params, opts);
    }
    try {
        let id = active.id;
        ui.procResultForm.reset();
        ui.procResultForm.update({ id });
        ui.procResultLoading.show();
        ui.procResultTree.removeAll();
        ui.procResultTable.removeAll();
        ui.procResultDialog.show();
        let promise = callProc(active.id);
        ui.procResultDialog.on('hide', () => promise.cancel());
        let xhr = await promise;
        let text = xhr.response;
        let json = JSON.parse(xhr.response);
        let size = approxSize(+xhr.getResponseHeader('Content-Length'));
        let duration = approxDuration(json.execTime);
        ui.procResultForm.update({ size, duration, text });
        if (json.error) {
            ui.procResultForm.update({ format: 'raw' });
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

async function edit(ui, data) {
    let createOption = (id) => create('option', { value: id }, id.replace('procedure/', ''));
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
        let isArg = b.type == 'argument';
        let help = def ? def.description : (!isArg && b.description) || '';
        let input = RapidContext.Widget.TextArea({
            name: `binding.${ b.name}`,
            value: isArg ? b.description : b.value,
            placeholder: placeholders[b.type],
            autosize: true,
            rows: 1,
            wrap: 'off',
            'class': 'flex-fill',
        });
        let up = def ? '' : buildBtn('fa fa-lg fa-level-down', 'down');
        let rm = def ? '' : buildBtn('fa fa-lg fa-minus', 'remove');
        let tr = tpl.render({ name: b.name, help })[0];
        tr.lastChild.firstChild.append(input, up, rm);
        return tr;
    }
    let data = ui.procEditForm.data;
    let type = typePath(data.type).pop();
    ui.procEditTypeHelp.replaceChildren(type && type.description || '');
    let tpls = {
        connection: ui.procEditConTpl,
        data: ui.procEditDataTpl,
        procedure: ui.procEditProcTpl,
        argument: ui.procEditArgTpl
    };
    for (let type in tpls) {
        tpls[type].clear();
    }
    let defaults = object('name', type && type.binding || []);
    data.bindings.forEach((b) => buildRow(tpls[b.type], b, defaults[b.name]));
    for (let type in tpls) {
        let el = tpls[type].closest('fieldset');
        el.classList.toggle('hidden', tpls[type].copies().length == 0);
    }
    let isTypeJs = data.type == 'procedure/javascript';
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
    let data = ui.procEditForm.data;
    let values = ui.procEditForm.valueMap();
    let type = typePath(values.type).pop();
    let bindings = object('name', type.binding || []);
    for (let b of data.bindings) {
        let key = (b.type == 'argument') ? 'description' : 'value';
        let val = (values[`binding.${b.name}`] || '').trim();
        // Exclude default bindings from old procedure type
        if (val || !b.description) {
            bindings[b.name] = {
                ...bindings[b.name],
                name: b.name,
                type: b.type,
                [key]: val,
            };
        }
    }
    data.id = values.id;
    data.type = values.type;
    data.description = values.description;
    data.bindings = Object.values(bindings);
    editRender(ui);
}

function editAction(ui, el) {
    let data = ui.procEditForm.data;
    if (el.dataset.action === 'add') {
        let type = ui.procEditAddBinding.value;
        let name = ui.procEditAddName.value.trim();
        ui.procEditAddValidator1.reset();
        ui.procEditAddValidator2.reset();
        if (!/^[a-z0-9_-]+$/i.test(name)) {
            ui.procEditAddValidator1.addError(ui.procEditAddName);
        } else if (data.bindings.find((b) => b.name == name)) {
            ui.procEditAddValidator2.addError(ui.procEditAddName);
        } else {
            ui.procEditAddName.setAttrs({ value: '' });
            let key = (type == 'argument') ? 'description' : 'value';
            data.bindings.push({ name, type, [key]: '' });
            editRefresh(ui);
            ui.procEditForm.querySelector(`textarea[name="binding.${name}"]`).focus();
        }
    } else if (el.dataset.action === 'remove') {
        let cur = el.closest('[data-binding]');
        data.bindings = data.bindings.filter((b) => b.name != cur.dataset.binding);
        cur.remove();
    } else if (el.dataset.action === 'down') {
        let cur = el.closest('[data-binding]');
        let next = cur.nextElementSibling;
        if (next && next.matches('[data-binding]')) {
            let ix1 = data.bindings.findIndex((b) => b.name == cur.dataset.binding);
            let ix2 = data.bindings.findIndex((b) => b.name == next.dataset.binding);
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
        let orig = ui.procEditForm.original;
        let data = ui.procEditForm.data;
        let oldPath = orig.id ? RapidContext.Storage.path(orig) : null;
        let newPath = RapidContext.Storage.path(data);
        data.binding = data.bindings.map((b) => {
            let res = { name: b.name, type: b.type, value: b.value };
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
        await refresh(ui);
        let node = ui.procTree.findByPath(data.id.split(/[./]/g));
        if (node && !node.isSelected()) {
            node.select();
        }
    } catch (e) {
        RapidContext.UI.showError(e);
    } finally {
        ui.procEditForm.querySelectorAll('button').forEach((el) => el.disabled = false);
    }
}

async function remove(ui) {
    try {
        await msg.warning.remove('procedure', active.id);
        let path = RapidContext.Storage.path(active);
        await RapidContext.App.callProc('system/storage/delete', [path]);
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
            let s = Object.prototype.toString.call(val);
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
            let lines = String(val).split('\n');
            let suffix = (lines.length > 1) ? '\u2026' : '';
            return `[${typeName(val)}] ${lines[0]}${suffix}`;
        }
    }
    if (node.getChildNodes().length > 0) {
        // Do nothing
    } else if (Array.isArray(data) || isObject(data)) {
        for (let k in data) {
            let v = /^\d+$/.test(k) ? data[+k] : data[k];
            let name = `${k}: ${label(v)}`;
            let folder = Array.isArray(v) || isObject(v);
            let extra = folder ? {} : { tooltip: String(v) };
            let child = RapidContext.Widget.TreeNode({ name, folder, ...extra });
            child.data = v;
            node.addAll(child);
        }
    } else {
        let name = label(data);
        let tooltip = String(data);
        let child = RapidContext.Widget.TreeNode({ name, tooltip });
        node.addAll(child);
    }
}

function renderDataTable(table, data) {
    if (isObject(data)) {
        data = Object.keys(data).map((k) => {
            let v = data[k];
            return isObject(v) ? { key: k, ...v } : { key: k, value: v };
        });
    } else if (!Array.isArray(data)) {
        data = [data].filter(Boolean);
    }
    let arr = data.slice(0, 1000).map((o) => isObject(o) ? o : { value: o });
    let props = Object.keys(arr[0] || {}).slice(0, 10);
    let toCol = (k) => ({ field: k, title: RapidContext.Util.toTitleCase(k), maxLength: 40 });
    table.removeAll();
    table.addAll(props.map((k) => RapidContext.Widget.TableColumn(toCol(k))));
    table.setData(arr);
}

function swap(arr, ix1, ix2) {
    let el = arr[ix1];
    arr[ix1] = arr[ix2];
    arr[ix2] = el;
}
