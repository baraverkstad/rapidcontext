import { loadTypes, objectProps, renderProp, approxSize } from './util.mjs';

let plugins = [];

export default function init(ui) {
    ui.pluginPane.once('enter', () => refresh(ui));
    ui.pluginSearch.on('reload', () => refresh(ui));
    ui.pluginSearch.on('search', () => search(ui));
    ui.pluginFile.addEventListener('change', () => upload(ui));
    ui.pluginReset.on('click', () => reset(ui));
    ui.pluginTable.on('select', () => show(ui));
    ui.pluginDetails.on('unselect', () => ui.pluginTable.setSelectedIds());
    ui.pluginLoad.on('click', () => load(ui));
    ui.pluginUnload.on('click', () => unload(ui));
    ui.pluginRemove.on('click', () => uninstall(ui));
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
    const q = ui.pluginSearch.query;
    const data = plugins.filter((o) => {
        const s = `${o.id}#${o.name}#${o.version}#${o._content.join('#')}#${o.description}`;
        return s.toLowerCase().includes(q);
    });
    ui.pluginSearch.info(data.length, plugins.length, 'plug-ins');
    ui.pluginTable.setData(data);
}

function show(ui) {
    const data = ui.pluginTable.getSelectedData();
    if (data) {
        ui.pluginIdLink.value = data.id;
        const ignore = ['id', 'className', '_loaded', '_builtin'];
        const props = objectProps(data, ignore).map((p) => renderProp(p, data));
        ui.pluginPropTpl.clear();
        ui.pluginPropTpl.render(props);
        const isRequired = data.id === 'system' || data.id === 'local';
        ui.pluginLoad.setAttrs({ hidden: data._loaded || isRequired });
        ui.pluginUnload.setAttrs({ hidden: !data._loaded || isRequired });
        ui.pluginRemove.setAttrs({ hidden: data._loaded || data._builtin || isRequired });
        ui.pluginDetails.show();
    } else {
        ui.pluginDetails.hide();
    }
}

async function upload(ui) {
    const file = ui.pluginFile.files[0];
    const size = approxSize(file.size);
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
        const pluginId = await RapidContext.App.callProc('system/plugin/install', ['plugin']);
        await RapidContext.App.callProc('system/reset', []);
        await refresh(ui);
        ui.pluginTable.setSelectedIds(pluginId);
        postInstall(ui, pluginId);
    } catch (e) {
        RapidContext.UI.showError(e);
    } finally {
        ui.pluginProgress.hide();
        ui.pluginInstall.classList.remove('widgetDisabled');
        ui.pluginFile.disabled = false;
        ui.pluginReset.enable();
    }
}

async function postInstall(ui, pluginId) {
    const data = ui.pluginTable.getSelectedData();
    const proc = data['post-install'];
    if (data.id === pluginId && proc) {
        try {
            await RapidContext.UI.Msg.info({
                title: 'Post-Install Procedure',
                html: `
                    <p>The <code>${pluginId}</code> plug-in provides a post-install
                    procedure:<p>
                    <pre>${proc}</pre>
                    <p>Do you want to review this procedure now?</p>
                `.trim(),
                actions: {
                    cancel: {
                        icon: 'fa fa-lg fa-times',
                        text: 'No, thank you',
                    },
                    info: {
                        icon: 'fa fa-lg fa-arrow-right',
                        text: 'Yes, show procedure',
                    }
                }
            });
            ui.root.emit('settings-proc-select', { detail: proc });
        } catch (e) {
            // Do nothing if cancelled
        }
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
    const data = ui.pluginTable.getSelectedData();
    ui.overlay.setAttrs({ loading: true, message: msg });
    ui.overlay.show();
    try {
        ui.pluginTable.setSelectedIds(); // Clear selection
        await RapidContext.App.callProc(proc, [data.id]);
        await reset(ui);
        ui.pluginTable.setSelectedIds(data.id);
        if (data._loaded && data._content.includes('lib')) {
            const msg = 'Unloading Java resources require a full server restart.';
            RapidContext.UI.Msg.info(msg);
        }
    } catch (e) {
        RapidContext.UI.showError(e);
    }
    ui.overlay.hide();
}
