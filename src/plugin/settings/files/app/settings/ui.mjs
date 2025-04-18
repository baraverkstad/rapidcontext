import { object } from 'rapidcontext/data';
import { hasValue } from 'rapidcontext/fn';
import { create } from 'rapidcontext/ui';
import { escape, template, approxDuration } from './util.mjs';

function uid(prefix) {
    const num = Math.round(Math.random() * 0xffffffff);
    return `${prefix}-${num.toString(16).padStart(8, '0')}`;
}

class Template {
    static build(attrs, ...children) {
        const o = document.createElement('template');
        RapidContext.Widget._widgetMixin(o, Template);
        o.setAttrs({ 'data-tpl': uid('tpl') });
        o.addAll(children);
        return o;
    }

    _containerNode() {
        return this.content;
    }

    clear() {
        this.copies().forEach((el) => el.remove());
    }

    copies() {
        const res = [];
        let el = this.previousSibling;
        while (el && el.classList.contains(this.dataset.tpl)) {
            res.unshift(el);
            el = el.previousSibling;
        }
        return res;
    }

    render(data) {
        if (Array.isArray(data)) {
            return data.filter(Boolean).map((o) => this.render(o));
        } else {
            const res = [];
            this.content.childNodes.forEach((el) => {
                const html = template(el.outerHTML, data);
                this.insertAdjacentHTML('beforebegin', html);
                this.previousSibling.classList.add(this.dataset.tpl);
                res.push(this.previousSibling);
            });
            return res;
        }
    }
}


class IdLink {
    static build(attrs, ...children) {
        const ui = `
            <div>
                <span class="help-text">type</span>
                <b></b>
                <a href="#" target="_blank" title="Open in storage" class="btn reactive" style="margin: -0.3em 0;">
                    <i class="fa fa-sitemap" />
                </a>
            </div>
        `;
        const o = create(ui.trim());
        RapidContext.Widget._widgetMixin(o, IdLink);
        o.separator = '<span class="separator">/</span>';
        o.setAttrs({ url: 'rapidcontext/storage/{{type}}/{{value}}', ...attrs });
        o.addAll(children);
        return o;
    }

    get type() {
        const path = this.querySelector('div > span').innerText;
        return path.split('/').filter(Boolean)[0];
    }

    set type(val) {
        const base = String(val).split('/')[0];
        const html = ['', base, ''].map(escape).join(this.separator);
        this.querySelector('div > span').innerHTML = html;
    }

    get value() {
        return this.querySelector('b').innerText;
    }

    set value(val) {
        const html = String(val).split('/').map(escape).join(this.separator);
        this.querySelector('b').innerHTML = html;
        this.querySelector('a').setAttribute('href', template(this.url, this));
    }
}


class Toggle {
    static build(attrs, ...children) {
        const ui = `
            <label class="toggle">
                <input type="checkbox"/>
                <i />
                <span />
            </label>
        `;
        const o = create(ui.trim());
        RapidContext.Widget._widgetMixin(o, Toggle);
        o.classList.add('toggle');
        o.setAttrs(attrs);
        o.querySelector('span').append(...children);
        return o;
    }

    setAttrs(attrs) {
        const { name, value, checked, ...other } = attrs;
        const input = this.querySelector('input');
        hasValue(name) && input.setAttribute('name', name);
        hasValue(value) && input.setAttribute('value', value);
        hasValue(checked) && input.setAttribute('checked', checked);
        this.__setAttrs(other);
        this.classList.add('toggle'); // FIXME: filter 'class' attribute values instead
    }
}


class SearchForm {
    static build(attrs, ...children) {
        const ui = `
            <Form class="flex-row flex-align-center mb-2">
                <div class="flex-fill flex-row flex-align-center">
                    <TextField type="search" name="search" w="25em" class="flex-fill m-0" style="max-width: 25em;" />
                    <Button name="reload" icon="fa fa-refresh" class="reactive" title="Reload" data-action="reload" />
                    <Icon class="fa fa-spin fa-refresh ml-1" />
                    <div class="flex-fill mx-2"></div>
                </div>
            </Form>
        `;
        const o = create(ui.trim());
        RapidContext.Widget._widgetMixin(o, SearchForm);
        o.setAttrs({ loading: false, ...attrs });
        o.addAll(children);
        o.on('input', 'input[type="search"]', () => o.emit('search'), { delay: 300 });
        o.on('click', 'button[data-action="reload"]', () => o.emit('reload'));
        return o;
    }

    get query() {
        return this.search.value.toLowerCase().trim();
    }

    set query(val) {
        this.search.value = val;
    }

    get loading() {
        return this.reload.isHidden();
    }

    set loading(val) {
        this.reload.setAttrs({ hidden: !!val });
        this.querySelector('div > i.fa-spin').setAttrs({ hidden: !val });
    }

    get placeholder() {
        return this.search.placeholder;
    }

    set placeholder(val) {
        this.search.placeholder = val;
    }

    set datalist(items) {
        const list = create('datalist', { id: uid('list') });
        items = Array.isArray(items) ? object(items, items) : items;
        for (const k in items) {
            const v = items[k];
            const attrs = (k == v) ? { value: k } : { value: k, label: v };
            list.append(create('option', attrs));
        }
        this.search.insertAdjacentElement('afterend', list);
        this.search.setAttrs({ list: list.id });
    }

    info(count, total, type) {
        const html = `Showing ${count} <span class="unimportant">(of ${total})</span> ${type}`;
        this.querySelector('div > div').innerHTML = html;
    }
}


class DetailsForm {
    static build(attrs, ...children) {
        const ui = `
            <Form class="overflow-y-auto border-l ml-2 pt-1 pl-2 position-relative">
                <Button data-action="hide" class="reactive position-absolute top-0 left-0 mt-0">
                    <i class="fa fa-chevron-right" style="margin-left: 0.1em;"></i>
                </Button>
            </Form>
        `;
        const o = create(ui.trim());
        RapidContext.Widget._widgetMixin(o, DetailsForm);
        o.setAttrs({ hidden: true, ...attrs });
        o.addAll(children);
        o.on('click', 'button[data-action="hide"]', () => o.emit('unselect'));
        return o;
    }
}


class MetricsDialog {
    static build(attrs, ...children) {
        function setRenderer(col, id) {
            function renderer(td, val, data) {
                const re = /([^0-9 ]+)/g;
                const wrap = (s) => re.test(s) ? create('span', { 'class': 'help-text' }, s) : s;
                td.append(...data[id].split(re).filter(Boolean).map(wrap));
            }
            col.setAttrs({ renderer });
        }
        const ui = `
            <Dialog title="Metrics" modal="true" w="75%" h="75%" class="flex-col">
                <Table class="metrics-table flex-fill">
                    <TableColumn title="Identifier" field="id" key="true" />
                    <TableColumn title="Usage" field="count" type="number" />
                    <TableColumn title="Errors" field="error" type="number" />
                    <TableColumn title="Avg. Time" field="avg" type="number" />
                    <TableColumn title="Total Time" field="total" type="number" sort="desc" />
                </Table>
                <div class="text-right mt-1">
                <Button icon="fa fa-lg fa-times" data-dialog="close">Close</Button>
                </div>
            </Dialog>
        `;
        const o = create(ui.trim());
        RapidContext.Widget._widgetMixin(o, MetricsDialog);
        o.setAttrs({ ...attrs });
        o.addAll(children);
        const table = o.querySelector('.metrics-table');
        const cols = table.getChildNodes();
        setRenderer(cols[1], 'count_text');
        setRenderer(cols[2], 'error_text');
        setRenderer(cols[3], 'avg_text');
        setRenderer(cols[4], 'total_text');
        return o;
    }

    async fetch(type) {
        try {
            const title = `${RapidContext.Util.toTitleCase(type)} Metrics`;
            this.setAttrs({ title });
            const data = await RapidContext.App.callProc(`system/${type}/metrics`);
            const arr = [];
            for (const id in data) {
                const o = data[id];
                const hasCnt = o.count?.month > 0;
                const hasErr = o.error?.month > 0;
                const hasAvg = Array.isArray(o.avg) && o.avg.length == 3;
                arr.push({
                    id,
                    count: hasCnt ? o.count.month : 0,
                    count_text: hasCnt ? `${o.count.hour} / ${o.count.day} / ${o.count.month} hour/day/mon` : '',
                    error: hasErr ? o.error.month : 0,
                    error_text: hasErr ? `${o.error.hour} / ${o.error.day} / ${o.error.month} hour/day/mon` : '',
                    avg: hasAvg ? o.avg[2] : 0,
                    avg_text: hasAvg ? approxDuration(o.avg[2]) : '',
                    total: (hasCnt && hasAvg) ? o.count.month * o.avg[2] : 0,
                    total_text: (hasCnt && hasAvg) ? approxDuration(o.count.month * o.avg[2]) : '',
                });
            }
            const table = this.querySelector('.metrics-table');
            table.setData(arr);
            this.show();
        } catch (e) {
            RapidContext.UI.showError(e);
        }
    }
}


RapidContext.Widget.Classes['settings:template'] = Template.build;
RapidContext.Widget.Classes['settings:id-link'] = IdLink.build;
RapidContext.Widget.Classes['settings:toggle'] = Toggle.build;
RapidContext.Widget.Classes['settings:search-form'] = SearchForm.build;
RapidContext.Widget.Classes['settings:details-form'] = DetailsForm.build;
RapidContext.Widget.Classes['settings:metrics-dialog'] = MetricsDialog.build;
