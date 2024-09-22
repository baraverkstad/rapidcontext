import { object } from 'rapidcontext/data';
import { hasValue } from 'rapidcontext/fn';
import { create } from 'rapidcontext/ui';
import { escape, template } from './util.mjs';

function uid(prefix) {
    let num = Math.round(Math.random() * 0xffffffff);
    return `${prefix}-${num.toString(16).padStart(8, '0')}`;
}

class Template {
    static build(attrs, ...children) {
        let o = document.createElement('template');
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
        let res = [];
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
            let res = [];
            this.content.childNodes.forEach((el) => {
                let html = template(el.outerHTML, data);
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
        let o = create(IdLink.UI.trim());
        RapidContext.Widget._widgetMixin(o, IdLink);
        o.setAttrs({ url: 'rapidcontext/storage/{{type}}/{{value}}', ...attrs });
        o.addAll(children);
        return o;
    }

    get type() {
        let path = this.querySelector('div > span').innerText;
        return path.split('/').filter(Boolean)[0];
    }

    set type(val) {
        let base = String(val).split('/')[0];
        let html = ['', base, ''].map(escape).join(IdLink.SEPARATOR);
        this.querySelector('div > span').innerHTML = html;
    }

    get value() {
        return this.querySelector('b').innerText;
    }

    set value(val) {
        let html = String(val).split('/').map(escape).join(IdLink.SEPARATOR);
        this.querySelector('b').innerHTML = html;
        this.querySelector('a').setAttribute('href', template(this.url, this));
    }
}

IdLink.SEPARATOR = '<span class="separator">/</span>';
IdLink.UI = `
    <div>
        <span class="help-text">type</span>
        <b></b>
        <a href="#" target="_blank" title="Open in storage" class="btn reactive" style="margin: -0.3em 0;">
            <i class="fa fa-sitemap" />
        </a>
    </div>
`;


class Toggle {
    static build(attrs, ...children) {
        let o = create(Toggle.UI.trim());
        RapidContext.Widget._widgetMixin(o, Toggle);
        o.classList.add('toggle');
        o.setAttrs(attrs);
        o.querySelector('span').append(...children);
        return o;
    }

    setAttrs(attrs) {
        let { name, value, checked, ...other } = attrs;
        let input = this.querySelector('input');
        hasValue(name) && input.setAttribute('name', name);
        hasValue(value) && input.setAttribute('value', value);
        hasValue(checked) && input.setAttribute('checked', checked);
        this.__setAttrs(other);
        this.classList.add('toggle'); // FIXME: filter 'class' attribute values instead
    }
}

Toggle.UI = `
    <label class="toggle">
        <input type="checkbox"/>
        <i />
        <span />
    </label>
`;


class SearchForm {
    static build(attrs, ...children) {
        let o = create(SearchForm.UI.trim());
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
        let list = create('datalist', { id: uid('list') });
        items = Array.isArray(items) ? object(items, items) : items;
        for (let k in items) {
            let v = items[k];
            let attrs = (k == v) ? { value: k } : { value: k, label: v };
            list.append(create('option', attrs));
        }
        this.search.insertAdjacentElement('afterend', list);
        this.search.setAttrs({ list: list.id });
    }

    info(count, total, type) {
        let html = `Showing ${count} <span class="unimportant">(of ${total})</span> ${type}`;
        this.querySelector('div > div').innerHTML = html;
    }
}

SearchForm.UI = `
    <Form class="flex-row flex-align-center mb-2">
        <div class="flex-fill flex-row flex-align-center">
            <TextField type="search" name="search" w="25em" class="flex-fill m-0" style="max-width: 25em;" />
            <Button name="reload" icon="fa fa-refresh" class="reactive" title="Reload" data-action="reload" />
            <Icon class="fa fa-spin fa-refresh ml-1" />
            <div class="flex-fill mx-2"></div>
        </div>
    </Form>
`;


class DetailsForm {
    static build(attrs, ...children) {
        let o = create(DetailsForm.UI.trim());
        RapidContext.Widget._widgetMixin(o, DetailsForm);
        o.setAttrs({ hidden: true, ...attrs });
        o.addAll(children);
        o.on('click', 'button[data-action="hide"]', () => o.emit('unselect'));
        return o;
    }
}

DetailsForm.UI = `
    <Form class="overflow-y-auto border-l ml-2 pt-1 pl-2 position-relative">
        <Button data-action="hide" class="reactive position-absolute top-0 left-0 mt-0">
            <i class="fa fa-chevron-right" style="margin-left: 0.1em;"></i>
        </Button>
    </Form>
`;


RapidContext.Widget.Classes['settings:template'] = Template.build;
RapidContext.Widget.Classes['settings:id-link'] = IdLink.build;
RapidContext.Widget.Classes['settings:toggle'] = Toggle.build;
RapidContext.Widget.Classes['settings:search-form'] = SearchForm.build;
RapidContext.Widget.Classes['settings:details-form'] = DetailsForm.build;
