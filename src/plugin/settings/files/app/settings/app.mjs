import initPluginTab from './tab-plugin.mjs';
import initConnectionTab from './tab-connection.mjs';
import initProcedureTab from './tab-procedure.mjs';
import initUserTab from './tab-user.mjs';
import { loadTypes } from './util.mjs';

export default class {
    async start() {
        // Initialize shared data
        try {
            await loadTypes();
        } catch (e) {
            RapidContext.UI.showError(e);
        }

        // Initialize tab UI
        // FIXME: Support non-admin users with disabled tabs
        await initPluginTab(this.ui);
        await initConnectionTab(this.ui);
        await initProcedureTab(this.ui);
        await initUserTab(this.ui);

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

    selectTab(target) {
        if (target) {
            let view = this.ui[target.dataset.view];
            this.ui.tabs.querySelectorAll('div[data-view]').forEach((el) => {
                el.classList.toggle('selected', el == target);
            });
            this.ui.views.querySelectorAll('.view').forEach((el) => {
                el.classList.toggle('hidden', el != view);
            });
            view && view.dispatchEvent(new CustomEvent('enter'));
        }
    }
}
