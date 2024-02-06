import { loadTypes, objectProps, renderProp } from './util.mjs';

let plugins = [];

export default function init(ui) {
    ui.pluginPane.addEventListener('enter', () => refresh(ui), { once: true });
    ui.pluginSearch.addEventListener('reload', () => refresh(ui));
    ui.pluginSearch.addEventListener('search', () => search(ui));
    ui.pluginFile.addEventListener('change', () => upload(ui));
    ui.pluginReset.addEventListener('click', () => reset(ui));
    ui.pluginTable.addEventListener('select', () => show(ui));
    ui.pluginDetails.addEventListener('unselect', () => ui.pluginTable.setSelectedIds());
    ui.pluginLoad.addEventListener('click', () => load(ui));
    ui.pluginUnload.addEventListener('click', () => unload(ui));
    ui.pluginRemove.addEventListener('click', () => uninstall(ui));
    ui.pluginTable.getChildNodes()[4].setAttrs({
        renderer: (td, val, data) => td.append(data._content.join(' \u2022 ')),
    });
}

async function refresh(ui) {
    ui.pluginSearch.loading = true;
    try {
        plugins = await RapidContext.App.callProc('system/plugin/list', []);
        search(ui);
    } catch (e) {
        RapidContext.UI.showError(e);
    }
    ui.pluginSearch.loading = false;
}

function search(ui) {
    let q = ui.pluginSearch.query;
    let data = plugins.filter((o) => {
        let s = `${o.id}#${o.name}#${o.version}#${o._content.join('#')}#${o.description}`;
        return s.toLowerCase().includes(q);
    });
    ui.pluginSearch.info(data.length, plugins.length, 'plug-ins');
    ui.pluginTable.setData(data);
}

function show(ui) {
    let data = ui.pluginTable.getSelectedData();
    if (data) {
        ui.pluginIdLink.value = data.id;
        let ignore = ['id', 'className', '_loaded', '_builtin'];
        let props = objectProps(data, ignore).map((p) => renderProp(p, data));
        ui.pluginPropTpl.clear();
        ui.pluginPropTpl.render(props);
        let isRequired = data.id === 'system' || data.id === 'local';
        ui.pluginLoad.setAttrs({ hidden: data._loaded || isRequired });
        ui.pluginUnload.setAttrs({ hidden: !data._loaded || isRequired });
        ui.pluginRemove.setAttrs({ hidden: data._loaded || data._builtin || isRequired });
        ui.pluginDetails.show();
    } else {
        ui.pluginDetails.hide();
    }
}

async function upload(ui) {
    function approxSize(size) {
        if (size > 1000000) {
            return MochiKit.Format.roundToFixed(size / 1048576, 1) + ' MiB';
        } else if (size > 2000) {
            return MochiKit.Format.roundToFixed(size / 1024, 1) + ' KiB';
        } else {
            return size + ' bytes';
        }
    }
    let file = ui.pluginFile.files[0];
    let size = approxSize(file.size);
    ui.pluginProgress.setAttrs({ min: 0, max: file.size, value: 0, text: size });
    ui.pluginProgress.show();
    ui.pluginInstall.classList.add('widgetDisabled');
    ui.pluginFile.disabled = true;
    ui.pluginReset.disable();
    try {
        ui.pluginTable.setSelectedIds(); // Clear selection
        await RapidContext.App.uploadFile('plugin', file, (evt) => {
            ui.pluginProgress.setAttrs({ value: evt.loaded });
        });
        let pluginId = await RapidContext.App.callProc('system/plugin/install', ['plugin']);
        await RapidContext.App.callProc('system/reset', []);
        await refresh(ui);
        ui.pluginTable.setSelectedIds(pluginId);
    } catch (e) {
        RapidContext.UI.showError(e);
    } finally {
        ui.pluginProgress.hide();
        ui.pluginInstall.classList.remove('widgetDisabled');
        ui.pluginFile.disabled = false;
        ui.pluginReset.enable();
    }
}

async function reset(ui) {
    ui.overlay.setAttrs({ loading: true, message: 'Resetting server...' });
    ui.overlay.show();
    try {
        await RapidContext.App.callProc('system/reset', []);
        await loadTypes();
        await refresh(ui);
    } catch (e) {
        RapidContext.UI.showError(e);
    } finally {
        ui.overlay.hide();
    }
}

async function load(ui) {
    await process(ui, 'system/plugin/load', 'Loading plug-in...');
}

async function unload(ui) {
    await process(ui, 'system/plugin/unload', 'Unloading plug-in...');
}

async function uninstall(ui) {
    await process(ui, 'system/plugin/uninstall', 'Removing plug-in...');
}

async function process(ui, proc, msg) {
    let data = ui.pluginTable.getSelectedData();
    ui.overlay.setAttrs({ loading: true, message: msg });
    ui.overlay.show();
    try {
        ui.pluginTable.setSelectedIds(); // Clear selection
        await RapidContext.App.callProc(proc, [data.id]);
        await reset(ui);
        ui.pluginTable.setSelectedIds(data.id);
        if (data._loaded && data._content.includes('lib')) {
            let msg = 'Unloading Java resources require a full server restart.';
            RapidContext.UI.Msg.info(msg);
        }
    } catch (e) {
        RapidContext.UI.showError(e);
    }
    ui.overlay.hide();
}
