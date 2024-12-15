import initPluginTab from './tab-plugin.mjs';
import initConnectionTab from './tab-connection.mjs';
import initProcedureTab from './tab-procedure.mjs';
import initUserTab from './tab-user.mjs';
import { loadTypes } from './util.mjs';

async function hasAccess(path, perm) {
    try {
        return await RapidContext.App.callProc('system/user/access', [path, perm, null]);
    } catch (e) {
        console.error('settings: failed to check', path, 'access', e);
        return false;
    }
}

export default class {
    async start() {
        // Initialize shared data
        try {
            await loadTypes();
        } catch (e) {
            RapidContext.UI.showError(e);
        }

        // Initialize views
        if (await hasAccess('/.storage/plugin/', 'search')) {
            await this.initTab('pluginPane', initPluginTab);
        }
        if (await hasAccess('/connection/', 'search')) {
            await this.initTab('cxnPane', initConnectionTab);
        }
        if (await hasAccess('/procedure/', 'search')) {
            await this.initTab('procPane', initProcedureTab);
        }
        if (await hasAccess('/user/', 'search')) {
            await this.initTab('userPane', initUserTab);
        }

        // Setup event handlers
        this.ui.tabs.on('click', '[data-view]:not(.disabled)', (evt) => {
            this.selectTab(evt.delegateTarget);
        });

        // Select first tab
        this.selectTab(this.ui.tabs.querySelector('[data-view]:not(.disabled)'));
    }

    stop() {
        // Do nothing here
    }

    async initTab(id, fn) {
        try {
            this.ui.tabs.querySelector(`div[data-view="${id}"]`).classList.remove('disabled');
            await fn(this.ui);
        } catch (e) {
            console.error(`settings: failed to init ${id} view`, e);
        }
    }

    selectTab(target) {
        let view = target && this.ui[target.dataset.view];
        this.ui.tabs.querySelectorAll('div[data-view]').forEach((el) => {
            el.classList.toggle('selected', el == target);
        });
        this.ui.views.querySelectorAll('.view').forEach((el) => {
            el.classList.toggle('hidden', el != view);
        });
        view && view.dispatchEvent(new CustomEvent('enter'));
    }
}
