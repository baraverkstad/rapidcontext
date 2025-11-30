import { startCase } from 'rapidcontext/text';
import { create } from 'rapidcontext/ui';
import { str, typeIds, typePath, objectProps, renderProp } from './util.mjs';

let connections = [];

export default function init(ui) {
    ui.cxnPane.once('enter', () => refresh(ui));
    ui.cxnSearch.on('reload', () => refresh(ui));
    ui.cxnSearch.on('search', () => search(ui));
    ui.cxnValidateAll.on('click', () => validateAll(ui));
    ui.cxnAdd.on('click', () => edit(ui, {}));
    ui.cxnMetrics.on('click', () => ui.cxnMetricsDialog.fetch('connection'));
    ui.cxnTable.on('select', () => show(ui));
    ui.cxnDetails.on('unselect', () => ui.cxnTable.setSelectedIds());
    ui.cxnValidate.on('click', () => validate(ui));
    ui.cxnEdit.on('click', () => edit(ui, ui.cxnTable.getSelectedData()));
    ui.cxnDelete.on('click', () => remove(ui));
    ui.cxnEditType.addEventListener('change', () => editRender(ui));
    ui.cxnEditShowAll.addEventListener('change', () => ui.cxnEditTable.classList.toggle('show-defaults'));
    ui.cxnEditForm.on('click', '[data-action]', (evt) => editAction(ui, evt.delegateTarget));
    ui.cxnEditForm.on('submit', (evt) => save(ui, evt));
    ui.cxnTable.getChildNodes()[1].setAttrs({ renderer: typeRenderer });
    ui.cxnTable.getChildNodes()[3].setAttrs({ renderer: statusRenderer });
    ui.cxnTable.getChildNodes()[4].setAttrs({ renderer: openRenderer });
}

function typeRenderer(td, value, data) {
    td.append(value.replace(/^connection\//, ''));
}

function statusRenderer(td, value, data) {
    const usedAt = data?._lastUsedTime ?? '@0';
    const since = Date.now() - parseInt(usedAt.substr(1), 10);
    let cls = 'fa fa-check';
    if (data?._loading) {
        cls = 'fa fa-spin fa-refresh';
    } else if (data?._error || data?._lastError) {
        cls = 'fa fa-exclamation-circle color-danger';
    } else if (!data?._openChannels && since > 5 * 60 * 1000) {
        cls = 'fa fa-exclamation-triangle color-warning';
    }
    td.append(RapidContext.Widget.Icon({ 'class': cls }));
}

function openRenderer(td, value, data) {
    const arr = [data._usedChannels, data._openChannels, data._maxOpen];
    td.innerHTML = arr.join('<span class="separator">/</span>');
    if (data._openChannels >= data._maxOpen) {
        td.classList.add('color-danger', 'bg-danger');
    } else if (data._openChannels >= data._maxOpen * 0.75) {
        td.classList.add('color-warning', 'bg-warning');
    }
}

async function refresh(ui) {
    ui.cxnSearch.loading = true;
    try {
        const selected = ui.cxnTable.getSelectedIds();
        ui.cxnTable.setSelectedIds(); // clear selection
        connections = await RapidContext.App.callProc('system/connection/list', []);
        search(ui);
        ui.cxnTable.setSelectedIds(selected);
    } catch (e) {
        RapidContext.UI.showError(e);
    }
    ui.cxnSearch.loading = false;
}

function search(ui) {
    const q = ui.cxnSearch.query;
    const data = connections.filter((o) => {
        const s = `${o.id}#${o.type}#${o.plugin}#${o.description}`;
        return s.toLowerCase().includes(q);
    });
    ui.cxnSearch.info(data.length, connections.length, 'connections');
    ui.cxnTable.setData(data);
}

function show(ui) {
    const data = ui.cxnTable.getSelectedData();
    if (data) {
        ui.cxnIdLink.value = data.id;
        const ignore = ['id', 'className'];
        const props = objectProps(data, ignore).map((p) => renderProp(p, data));
        ui.cxnPropTpl.clear();
        ui.cxnPropTpl.render(props);
        ui.cxnDelete.setAttrs({ hidden: data._plugin != 'local' });
        ui.cxnDetails.show();
    } else {
        ui.cxnDetails.hide();
    }
}

async function validate(ui, con) {
    let data = con ?? ui.cxnTable.getSelectedData();
    ui.cxnTable.updateData({ _loading: true, ...data });
    try {
        data = await RapidContext.App.callProc('system/connection/validate', [data.id]);
        ui.cxnTable.updateData(data);
    } catch (e) {
        data._lastError = String(e);
        ui.cxnTable.updateData(data);
    }
    if (!con) {
        await refresh(ui);
    }
}

async function validateAll(ui) {
    const data = ui.cxnTable.getData();
    await Promise.allSettled(data.map((o) => validate(ui, o)));
    await refresh(ui);
}

function edit(ui, data) {
    const createOption = (id) => create('option', { value: id }, id.replace('connection/', ''));
    ui.cxnEditType.replaceChildren(ui.cxnEditType.firstChild);
    ui.cxnEditType.append(...typeIds('connection').map(createOption));
    ui.cxnEditTable.classList.remove('show-defaults');
    ui.cxnEditForm.reset();
    ui.cxnEditForm.update(data);
    ui.cxnEditForm.original = data;
    ui.cxnEditDialog.show();
    editRender(ui, data);
}

function editRender(ui, extra) {
    function buildInput(name, format, placeholder, required) {
        if (format == 'password') {
            const defs = { type: 'password', 'class': 'flex-fill' };
            return RapidContext.Widget.TextField({ name, ...defs });
        } else {
            const defs = { autosize: true, rows: 1, wrap: 'off', 'class': 'flex-fill' };
            return RapidContext.Widget.TextArea({ name, placeholder, required, ...defs });
        }
    }
    function buildBtn(icon, action) {
        return RapidContext.Widget.Button({
            icon,
            'class': 'font-smaller ml-1',
            'data-action': action,
        });
    }
    function buildRow(p) {
        const name = p.name;
        const title = p.title ?? startCase(name ?? '');
        const placeholder = str(data[`_${name}`]) || str(p.value) || '';
        const help = p.description || '';
        const input = buildInput(name, p.format, placeholder, p.required && p.format != 'text');
        const rm = p.custom ? buildBtn('fa fa-lg fa-minus', 'remove') : '';
        const tr = ui.cxnEditTpl.render({ name, title, help })[0];
        tr.lastChild.firstChild.append(input, rm);
        if (input.required) {
            const validator = RapidContext.Widget.FormValidator({ name });
            tr.lastChild.insertBefore(validator, tr.lastChild.lastChild);
        }
        if (!p.required && !p.custom && !data[name]) {
            tr.classList.add('default-prop');
        }
        return tr;
    }
    ui.cxnEditForm.defaults = extra ?? ui.cxnEditForm.defaults;
    const data = { ...ui.cxnEditForm.defaults, ...ui.cxnEditForm.valueMap() };
    ui.cxnEditForm.reset();
    const ignore = ['id', 'type', 'className'];
    const props = objectProps(data, ignore).filter((p) => !p.name.startsWith('_'));
    const desc = typePath(data.type).reverse()[0];
    ui.cxnEditTypeHelp.replaceChildren(desc?.description ?? '');
    ui.cxnEditTpl.clear();
    props.forEach(buildRow);
    ui.cxnEditForm.update(data);
    ui.cxnEditDialog.moveToCenter();
}

function editAction(ui, el) {
    if (el.dataset.action === 'add') {
        const data = ui.cxnEditForm.valueMap();
        const name = ui.cxnEditAddParam.value.trim();
        ui.cxnEditParamValidator1.reset();
        ui.cxnEditParamValidator2.reset();
        if (!/^[a-z0-9_-]+$/i.test(name)) {
            ui.cxnEditParamValidator1.addError(ui.cxnEditAddParam);
        } else if (name in data) {
            ui.cxnEditParamValidator2.addError(ui.cxnEditAddParam);
        } else {
            editRender(ui, { [name]: 'value' });
            ui.cxnEditAddParam.setAttrs({ value: '' });
            ui.cxnEditForm.querySelector(`textarea[name="${name}"]`).focus();
        }
    } else if (el.dataset.action === 'remove') {
        RapidContext.Widget.destroyWidget(el.closest('tr'));
    }
}

async function save(ui, evt) {
    evt.preventDefault();
    ui.cxnEditForm.querySelectorAll('button').forEach((el) => el.disabled = true);
    try {
        const orig = ui.cxnEditForm.original;
        const data = ui.cxnEditForm.valueMap();
        const all = { ...orig, ...data };
        for (const k in all) {
            const v = (data[k] ?? '').trim();
            if (!v && orig.id && !k.startsWith('.')) {
                data[k] = null; // Remove previous value
            } else if (!v) {
                delete data[k]; // Omit value
            }
        }
        const oldPath = orig.id ? `/connection/${orig.id}` : null;
        const newPath = `/connection/${data.id}`;
        let opts = { path: `${newPath}.yaml` };
        if (oldPath && oldPath !== newPath) {
            opts = { path: oldPath, updateTo: opts.path };
        } else if (oldPath) {
            opts.update = true;
        }
        await RapidContext.App.callProc('system/storage/write', [opts, data]);
        ui.cxnEditDialog.hide();
        await refresh(ui);
        if (oldPath !== newPath) {
            ui.cxnTable.setSelectedIds(data.id);
        }
    } catch (e) {
        RapidContext.UI.showError(e);
    } finally {
        ui.cxnEditForm.querySelectorAll('button').forEach((el) => el.disabled = false);
    }
}

async function remove(ui) {
    try {
        const data = ui.cxnTable.getSelectedData();
        await RapidContext.UI.Msg.warning.remove('connection', data.id);
        await RapidContext.App.callProc('system/storage/delete', [`/connection/${data.id}`]);
        await refresh(ui);
    } catch (e) {
        // FIXME: Better detection of cancelled operation?
        if (e.message != 'operation cancelled') {
            RapidContext.UI.showError(e);
        }
    }
}
