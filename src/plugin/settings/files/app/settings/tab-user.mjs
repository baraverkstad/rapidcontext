import { objectProps, renderProp } from './util.mjs';

let users = [];
let roles = [];

export default async function init(ui) {
    ui.userPane.addEventListener('enter', () => refresh(ui), { once: true });
    ui.userSearch.addEventListener('reload', () => refresh(ui));
    ui.userSearch.addEventListener('search', () => search(ui));
    ui.userAdd.addEventListener('click', () => edit(ui, true));
    ui.userTable.addEventListener('select', () => show(ui));
    ui.userDetails.addEventListener('unselect', () => ui.userTable.setSelectedIds());
    ui.userEdit.addEventListener('click', () => edit(ui, false));
    ui.userForm.addEventListener('submit', (evt) => save(ui, evt));
    try {
        roles = await RapidContext.App.callProc('system/storage/read', ['/role/']);
        ui.userEditRoleTpl.render(roles.filter((r) => !r.auto));
    } catch (e) {
        RapidContext.UI.showError(e);
    }
}

async function refresh(ui) {
    ui.userSearch.loading = true;
    try {
        users = await RapidContext.App.callProc('system/user/list', []);
        users.forEach((u) => {
            u.admin = u.role.includes('admin');
            u.roleCount = u.role.length;
        });
        search(ui);
    } catch (e) {
        RapidContext.UI.showError(e);
    }
    ui.userSearch.loading = false;
}

function search(ui) {
    let q = ui.userSearch.query.toLowerCase().trim();
    let data = users.filter((o) => {
        let s = `${o.id}#${o.name}#${o.email}#${o.description}`;
        return s.toLowerCase().includes(q);
    });
    ui.userSearch.info(data.length, users.length, 'users');
    ui.userTable.setData(data);
}

function show(ui) {
    let data = ui.userTable.getSelectedData();
    if (data) {
        ui.userIdLink.value = data.id;
        let ignore = ['id', 'className', 'password', 'enabled', 'role', 'roleCount', 'admin', 'realm'];
        let props = objectProps(data, ignore).map((p) => renderProp(p, data));
        ui.userPropTpl.clear();
        ui.userPropTpl.render(props);
        ui.userEnabled.className = data.enabled ? 'fa fa-check-square' : 'fa fa-square-o';
        ui.userEnabledText.innerText = data.enabled ? 'Yes' : 'No';
        ui.userRoleTpl.clear();
        ui.userRoleTpl.render(roles.filter((r) => data.role.includes(r.id)));
        ui.userDetails.show();
    } else {
        ui.userDetails.hide();
    }
}

function edit(ui, create) {
    let data = create ? { enabled: true } : ui.userTable.getSelectedData();
    if (data) {
        ui.userForm.reset();
        ui.userForm.update(data);
        ui.userIdField.setAttrs({ readonly: create ? null : true });
        ui.userPwdField.setAttrs({ required: create ? true : null });
        ui.userPwdHint.classList.toggle('hidden', create);
        ui.userDialog.show();
    }
}

async function save(ui, evt) {
    evt.preventDefault();
    ui.userForm.querySelectorAll('button').forEach((el) => el.disabled = true);
    try {
        let data = ui.userForm.valueMap();
        let args = [
            data.id, data.name, data.email, data.description,
            data.enabled ? '1' : '0', data.password, data.role || []
        ];
        await RapidContext.App.callProc('system/user/change', args);
        ui.userDialog.hide();
        ui.userTable.setSelectedIds(); // Clear selection
        await refresh(ui);
        ui.userTable.setSelectedIds(data.id);
    } catch (e) {
        RapidContext.UI.showError(e);
    } finally {
        ui.userForm.querySelectorAll('button').forEach((el) => el.disabled = false);
    }
}
