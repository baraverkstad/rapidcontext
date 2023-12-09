import { hasValue } from 'rapidcontext/fn';
import { escape, template } from './util.mjs';


class Template {
    static create(attrs, ...children) {
        let o = document.createElement('template');
        RapidContext.Widget._widgetMixin(o, Template);
        let uid = Math.round(Math.random() * 0xffffff).toString(16).padStart(4, '0');
        o.setAttrs({ 'data-tpl': `tpl-${uid}` });
        o.addAll(children);
        return o;
    }

    _containerNode() {
        return this.content;
    }

    clear() {
        while (this.previousSibling && this.previousSibling.classList.contains(this.dataset.tpl)) {
            this.previousSibling.remove();
        }
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
    static create(attrs, ...children) {
        let o = RapidContext.UI.buildUI(IdLink.UI.trim());
        RapidContext.Widget._widgetMixin(o, IdLink);
        o.setAttrs({ url: 'rapidcontext/storage/{{type}}/{{value}}', ...attrs });
        o.addAll(children);
        return o;
    }

    get type() {
        let path = this.querySelector('[data-ref="type"]').innerText;
        return path.split('/').filter(Boolean)[0];
    }

    set type(val) {
        let base = String(val).split('/')[0];
        let html = ['', base, ''].map(escape).join(IdLink.SEPARATOR);
        this.querySelector('[data-ref="type"]').innerHTML = html;
    }

    get value() {
        return this.querySelector('[data-ref="id"]').innerText;
    }

    set value(val) {
        let html = String(val).split('/').map(escape).join(IdLink.SEPARATOR);
        this.querySelector('[data-ref="id"]').innerHTML = html;
        this.querySelector('a').setAttribute('href', template(this.url, this));
    }
}

IdLink.SEPARATOR = '<span class="separator">/</span>';
IdLink.UI = `
    <div>
        <span data-ref="type">type</span>
        <b data-ref="id"></b>
        <a href="#" target="_blank" title="Open in storage" class="btn reactive" style="margin: -0.3em 0;">
            <i class="fa fa-sitemap" />
        </a>
    </div>
`;


class Toggle {
    static create(attrs, ...children) {
        let o = RapidContext.UI.buildUI(Toggle.UI.trim());
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
    static create(attrs, ...children) {
        let o = RapidContext.UI.buildUI(SearchForm.UI.trim());
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
        this.querySelector('[data-ref="loading"]').setAttrs({ hidden: !val });
    }

    get placeholder() {
        return this.search.placeholder;
    }

    set placeholder(val) {
        this.search.placeholder = val;
    }

    info(count, total, type) {
        let html = `Showing ${count} <span class="unimportant">(of ${total})</span> ${type}`;
        this.querySelector('[data-ref="info"]').innerHTML = html;
    }
}

SearchForm.UI = `
    <Form class="flex-row flex-align-center mb-2">
        <div class="flex-fill flex-row flex-align-center">
            <TextField type="search" name="search" w="25em" class="flex-fill m-0" style="max-width: 25em;" />
            <Button name="reload" icon="fa fa-refresh" class="reactive" title="Reload" data-action="reload" />
            <Icon data-ref="loading" class="fa fa-spin fa-refresh ml-1" />
            <div data-ref="info" class="flex-fill mx-2"></div>
        </div>
    </Form>
`;


class DetailsForm {
    static create(attrs, ...children) {
        let o = RapidContext.UI.buildUI(DetailsForm.UI.trim());
        RapidContext.Widget._widgetMixin(o, DetailsForm);
        o.setAttrs({ hidden: true, ...attrs });
        o.addAll(children);
        o.on('click', 'button[data-action="hide"]', () => o.hide());
        return o;
    }
}

DetailsForm.UI = `
    <Form class="overflow-y-auto border-l ml-2 pt-1 pl-2 position-relative">
        <Button data-action="hide" class="reactive position-absolute top-0 left-0">
            <i class="fa fa-chevron-right" style="margin-left: 0.1em;"></i>
        </Button>
    </Form>
`;


RapidContext.Widget.Classes['settings:template'] = Template.create;
RapidContext.Widget.Classes['settings:id-link'] = IdLink.create;
RapidContext.Widget.Classes['settings:toggle'] = Toggle.create;
RapidContext.Widget.Classes['settings:search-form'] = SearchForm.create;
RapidContext.Widget.Classes['settings:details-form'] = DetailsForm.create;
