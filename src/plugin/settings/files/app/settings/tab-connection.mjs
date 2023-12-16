import { elem, typeIds, typePath, objectProps, renderProp } from './util.mjs';

let connections = [];

export default async function init(ui) {
    ui.cxnPane.addEventListener('enter', () => refresh(ui), { once: true });
    ui.cxnSearch.on('reload', () => refresh(ui));
    ui.cxnSearch.on('search', () => search(ui));
    ui.cxnValidateAll.on('click', () => validateAll(ui));
    ui.cxnAdd.on('click', () => edit(ui, true));
    ui.cxnTable.addEventListener('select', () => show(ui));
    ui.cxnValidate.addEventListener('click', () => validate(ui));
    ui.cxnEdit.addEventListener('click', () => edit(ui, false));
    ui.cxnDelete.addEventListener('click', () => remove(ui));
    ui.cxnEditType.addEventListener('change', () => renderEdit(ui));
    ui.cxnEditShowAll.addEventListener('change', () => renderEdit(ui));
    ui.cxnEditForm.addEventListener('click', (evt) => addRemoveProps(ui, evt));
    ui.cxnEditForm.addEventListener('submit', (evt) => save(ui, evt));
    ui.cxnTable.getChildNodes()[1].setAttrs({ renderer: typeRenderer });
    ui.cxnTable.getChildNodes()[3].setAttrs({ renderer: statusRenderer });
    ui.cxnTable.getChildNodes()[4].setAttrs({ renderer: openRenderer });
}

function typeRenderer(td, value, data) {
    td.append(value.replace(/^connection\//, ''));
}

function statusRenderer(td, value, data) {
    let usedAt = data && data._lastUsedTime || '@0';
    let since = Date.now() - parseInt(usedAt.substr(1), 10);
    let cls = 'fa fa-check';
    if (data._loading) {
        cls = 'fa fa-spin fa-refresh';
    } else if (data._error || data._lastError) {
        cls = 'fa fa-exclamation-circle color-danger';
    } else if (!data._openChannels && since > 5 * 60 * 1000) {
        cls = 'fa fa-exclamation-triangle color-warning';
    }
    td.append(RapidContext.Widget.Icon({ 'class': cls }));
}

function openRenderer(td, value, data) {
    let arr = [data._usedChannels, data._openChannels, data._maxOpen];
    td.innerHTML = arr.join('<span class="separator">/</span>');
    if (data._openChannels >= data._maxOpen) {
        td.classList.add('color-danger bg-danger');
    } else if (data._openChannels >= data._maxOpen * 0.75) {
        td.classList.add('color-warning bg-warning');
    }
}

async function refresh(ui) {
    ui.cxnSearch.loading = true;
    try {
        let selected = ui.cxnTable.getSelectedIds();
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
    let q = ui.cxnSearch.query;
    let data = connections.filter((o) => {
        let s = `${o.id}#${o.type}#${o.plugin}#${o.description}`;
        return s.toLowerCase().includes(q);
    });
    ui.cxnSearch.info(data.length, connections.length, 'connections');
    ui.cxnTable.setData(data);
}

function show(ui) {
    let data = ui.cxnTable.getSelectedData();
    if (data) {
        ui.cxnIdLink.value = data.id;
        let ignore = ['id', 'className'];
        let props = objectProps(data, ignore).map((p) => renderProp(p, data));
        ui.cxnPropTpl.clear();
        ui.cxnPropTpl.render(props);
        ui.cxnDelete.setAttrs({ hidden: data._plugin != 'local' });
        ui.cxnDetails.show();
    } else {
        ui.cxnDetails.hide();
    }
}

async function validate(ui, con) {
    let data = con || ui.cxnTable.getSelectedData();
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
    let data = ui.cxnTable.getData();
    await Promise.allSettled(data.map((o) => validate(ui, o)));
    await refresh(ui);
}

async function edit(ui, create) {
    let data = create ? {} : ui.cxnTable.getSelectedData();
    if (data) {
        ui.cxnEditType.replaceChildren(ui.cxnEditType.firstChild);
        for (let id of typeIds('connection')) {
            ui.cxnEditType.append(elem('option', { value: id }, id));
        }
        ui.cxnEditForm.reset();
        ui.cxnEditForm.update(data);
        ui.cxnEditForm.original = data;
        ui.cxnEditDialog.show();
        renderEdit(ui);
    }
}

function renderEdit(ui) {
    function buildRow(p) {
        let title = p.title || RapidContext.Util.toTitleCase(p.name || '');
        let value = data[p.name] || '';
        let defaultValue = data['_' + p.name] || '';
        let lines = lineCount(value);
        let tr = ui.cxnEditTpl.render({ title })[0];
        let attrs = { name: p.name, size: 60 };
        if (p.required && p.format != 'password' && p.format != 'text') {
            attrs.required = true;
        }
        if (defaultValue) {
            attrs.placeholder = defaultValue;
        }
        let input = null;
        if (p.format == 'text' || lines > 1) {
            attrs.cols = 55;
            attrs.rows = Math.min(Math.max(1, lines), 20);
            attrs.autosize = true;
            input = RapidContext.Widget.TextArea(attrs);
        } else if (p.format == 'password') {
            attrs.type = 'password';
            input = RapidContext.Widget.TextField(attrs);
        } else {
            input = RapidContext.Widget.TextField(attrs);
        }
        tr.lastChild.append(input);
        if (p.custom) {
            input.size = 55;
            let btn = { icon: 'fa fa-lg fa-minus', 'class': 'font-smaller ml-1', 'data-action': 'remove' };
            tr.lastChild.append(RapidContext.Widget.Button(btn));
        }
        if (attrs.required) {
            tr.lastChild.append(RapidContext.Widget.FormValidator({ name: p.name }));
        }
        if (p.description) {
            let help = document.createElement('div');
            help.className = 'helptext text-pre-wrap pt-0';
            help.append(p.description);
            tr.lastChild.append(help);
        }
        if (!showAll && !p.required && !p.custom && !value) {
            tr.classList.add('hidden');
        }
        return tr;
    }

    let data = { ...ui.cxnEditForm.original, ...ui.cxnEditForm.valueMap() };
    ui.cxnEditForm.reset();
    let showAll = (data._showAll == 'yes');
    let ignore = ['id', 'type', 'className'];
    let props = objectProps(data, ignore).filter((p) => !p.name.startsWith('_'));
    let desc = typePath(data.type).reverse()[0];
    ui.cxnEditTypeHelp.replaceChildren(desc && desc.description || '');
    ui.cxnEditTpl.clear();
    props.forEach(buildRow);
    ui.cxnEditForm.update(data);
    ui.cxnEditDialog.moveToCenter();
}

function addRemoveProps(ui, evt) {
    let el = evt.target.closest('[data-action]');
    if (el && el.dataset.action === 'add') {
        let data = ui.cxnEditForm.valueMap();
        let name = ui.cxnEditAddParam.value.trim();
        ui.cxnEditParamValidator1.reset();
        ui.cxnEditParamValidator2.reset();
        if (!/^[a-z0-9_-]+$/i.test(name)) {
            ui.cxnEditParamValidator1.addError(ui.cxnEditAddParam);
        } else if (name in data) {
            ui.cxnEditParamValidator2.addError(ui.cxnEditAddParam);
        } else {
            ui.cxnEditAddParam.setAttrs({ name: name, value: 'value' });
            renderEdit(ui);
            ui.cxnEditAddParam.setAttrs({ name: '_add', value: '' });
        }
    } else if (el && el.dataset.action === 'remove') {
        RapidContext.Widget.destroyWidget(el.closest('tr'));
    }
}

// FIXME: implement this with modern UI
async function save(ui, evt) {
    evt.preventDefault();
    ui.cxnEditForm.querySelectorAll('button').forEach((el) => el.disabled = true);
    try {
        let orig = ui.cxnEditForm.original;
        let data = ui.cxnEditForm.valueMap();
        for (let k in data) {
            let v = data[k].trim();
            if (!v && orig.id && !k.startsWith('.')) {
                data[k] = null; // Remove previous value
            } else if (!v) {
                delete data[k]; // Omit value
            }
        }
        let oldPath = orig.id ? RapidContext.Storage.path(orig) : null;
        let newPath = RapidContext.Storage.path(data);
        let opts = { path: newPath + '.yaml' };
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
        let data = ui.cxnTable.getSelectedData();
        await RapidContext.UI.Msg.warning.remove('connection', data.id);
        let path = RapidContext.Storage.path(data);
        await RapidContext.App.callProc('system/storage/delete', [path]);
        await refresh(ui);
    } catch (e) {
        // FIXME: Better detection of cancelled operation?
        if (e.message != 'operation cancelled') {
            RapidContext.UI.showError(e);
        }
    }
}

function lineCount(str) {
    let count = 0;
    str.split(/\n|\r\n|\r/g).forEach((s) => {
        count += s.split(/.{1,60}\s/g).length;
    });
    return count;
}
