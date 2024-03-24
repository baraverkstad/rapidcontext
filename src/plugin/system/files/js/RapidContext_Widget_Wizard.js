/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || { Classes: {} };

/**
 * Creates a new wizard widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {...Pane} [child] the child Pane widgets
 *
 * @return {Widget} the widget DOM node
 *
 * @class The wizard widget class. Used to provide a sequence of pages, where
 *     the user can step forward and backward with buttons. Internally it uses
 *     a `<div>` HTML element containing Pane widgets that are hidden and shown
 *     according to the page transitions.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let page1 = RapidContext.Widget.Pane({ pageTitle: "The first step" });
 * ...
 * let page2 = RapidContext.Widget.Pane({ pageTitle: "The second step" });
 * ...
 * let attrs = { style: { width: "100%", height: "100%" } };
 * let exampleWizard = RapidContext.Widget.Wizard(attrs, page1, page2);
 * let exampleDialog = RapidContext.Widget.Dialog({ title: "Example Dialog" }, exampleWizard);
 *
 * @example <caption>User Interface XML</caption>
 * <Dialog id="exampleDialog" title="Example Dialog" w="80%" h="50%">
 *   <Wizard id="exampleWizard" style="width: 100%; height: 100%;">
 *     <Pane pageTitle="The first step">
 *       ...
 *     </Pane>
 *     <Pane pageTitle="The second step">
 *       ...
 *     </Pane>
 *   </Wizard>
 * </Dialog>
 */
RapidContext.Widget.Wizard = function (attrs/*, ... */) {
    let o = MochiKit.DOM.DIV(attrs);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Wizard);
    o.addClass("widgetWizard");
    o._selectedIndex = -1;
    o.append(MochiKit.DOM.H3({ "class": "widgetWizardTitle" }));
    let bCancel = RapidContext.Widget.Button(
        { icon: "fa fa-lg fa-times", "class": "mr-2", "data-action": "cancel" },
        "Cancel"
    );
    let bPrev = RapidContext.Widget.Button(
        { icon: "fa fa-lg fa-caret-left", "class": "mr-2", "data-action": "previous" },
        "Previous"
    );
    let bNext = RapidContext.Widget.Button(
        { "data-action": "next" },
        "Next",
        RapidContext.Widget.Icon({ class: "fa fa-lg fa-caret-right" })
    );
    let bDone = RapidContext.Widget.Button(
        { icon: "fa fa-lg fa-check", highlight: true, "data-action": "done" },
        "Finish"
    );
    bCancel.hide();
    let divAttrs = { "class": "widgetWizardButtons" };
    o.append(MochiKit.DOM.DIV(divAttrs, bCancel, bPrev, bNext, bDone));
    o._updateStatus();
    o.setAttrs(attrs);
    o.addAll(Array.from(arguments).slice(1));
    o.on("click", ".widgetWizardButtons [data-action]", o._handleBtnClick);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Wizard = RapidContext.Widget.Wizard;

/**
 * Emitted when a page transition is performed.
 *
 * @name RapidContext.Widget.Wizard#onchange
 * @event
 */

/**
 * Emitted when the user selects to cancel the page flow.
 *
 * @name RapidContext.Widget.Wizard#oncancel
 * @event
 */

/**
 * Emitted when the user has completed the page flow.
 *
 * @name RapidContext.Widget.Wizard#onclose
 * @event
 */

/**
 * Handles button click events.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Wizard.prototype._handleBtnClick = function (evt) {
    let action = evt.delegateTarget.dataset.action;
    if (typeof(this[action]) == "function") {
        this[action]();
    }
};

/**
 * Returns an array with all child pane widgets. Note that the array
 * is a real JavaScript array, not a dynamic NodeList.
 *
 * @return {Array} the array of child wizard page widgets
 */
RapidContext.Widget.Wizard.prototype.getChildNodes = function () {
    return Array.from(this.childNodes).slice(2);
};

/**
 * Adds a single child page widget to this widget. The child widget should be a
 * `RapidContext.Widget.Pane` widget, or a new one will be created where it
 * will be added.
 *
 * @param {Widget} child the page widget to add
 */
RapidContext.Widget.Wizard.prototype.addChildNode = function (child) {
    if (!RapidContext.Widget.isWidget(child, "Pane")) {
        child = RapidContext.Widget.Pane(null, child);
    }
    child.style.width = "100%";
    child.style.height = "calc(100% - 65px)";
    child.hide();
    this.append(child);
    child.style.position = "absolute";
    // TODO: remove hard-coded size here...
    MochiKit.Style.setElementPosition(child, { x: 0, y: 24 });
    if (this.getChildNodes().length == 1) {
        this.activatePage(0);
    } else {
        this._updateStatus();
    }
};

// TODO: handle removes by possibly selecting new page...

/**
 * Updates the wizard status indicators, such as the title and the
 * current buttons.
 */
RapidContext.Widget.Wizard.prototype._updateStatus = function () {
    let h3 = this.childNodes[0];
    let bCancel = this.childNodes[1].childNodes[0];
    let bPrev = this.childNodes[1].childNodes[1];
    let bNext = this.childNodes[1].childNodes[2];
    let bDone = this.childNodes[1].childNodes[3];
    let page = this.activePage();
    let status = RapidContext.Widget.Pane.FORWARD;
    let title = null;
    let info = "(No pages available)";
    let icon = "";
    if (page != null) {
        status = page.pageStatus || RapidContext.Widget.Pane.ANY;
        title = page.pageTitle;
        info = " (Step " + (this._selectedIndex + 1) + " of " +
               this.getChildNodes().length + ")";
    }
    if (status === RapidContext.Widget.Pane.WORKING) {
        bCancel.show();
        bPrev.hide();
        icon = { ref: "LOADING", "class": "widgetWizardWait" };
        icon = RapidContext.Widget.Icon(icon);
    } else {
        bCancel.hide();
        bPrev.show();
    }
    if (this._selectedIndex >= this.getChildNodes().length - 1) {
        bNext.hide();
        bDone.show();
    } else {
        bNext.show();
        bDone.hide();
    }
    bPrev.setAttrs({ disabled: (this._selectedIndex <= 0) || !status.previous });
    bNext.setAttrs({ disabled: !status.next });
    bDone.setAttrs({ disabled: !status.next });
    info = MochiKit.DOM.SPAN({ "class": "widgetWizardInfo" }, info);
    h3.innerHTML = "";
    h3.append(icon, title, info);
};

/**
 * Returns the active page.
 *
 * @return {Widget} the active page, or
 *         null if no pages have been added
 */
RapidContext.Widget.Wizard.prototype.activePage = function () {
    if (this._selectedIndex >= 0) {
        return this.childNodes[this._selectedIndex + 2];
    } else {
        return null;
    }
};

/**
 * Returns the active page index.
 *
 * @return the active page index, or
 *         -1 if no page is active
 */
RapidContext.Widget.Wizard.prototype.activePageIndex = function () {
    return this._selectedIndex;
};

/**
 * Activates a new page and sends the `onchange` signal. If the page is moved
 * forward, the old page must pass a form validation check, or nothing will
 * happen.
 *
 * @param {number|Widget} indexOrPage the page index or page DOM node
 *
 * @see #next
 * @see #previous
 */
RapidContext.Widget.Wizard.prototype.activatePage = function (indexOrPage) {
    let index, page;
    if (typeof(indexOrPage) == "number") {
        index = indexOrPage;
        page = this.childNodes[index + 2];
    } else {
        page = indexOrPage;
        index = Array.from(this.childNodes).indexOf(page, 2) - 2;
    }
    if (index < 0 || index >= this.getChildNodes().length) {
        throw new RangeError("Page index out of bounds: " + index);
    }
    let oldIndex = this._selectedIndex;
    let oldPage = this.activePage();
    if (oldPage != null && oldPage !== page) {
        if (!oldPage._handleExit({ hide: false, validate: this._selectedIndex < index })) {
            // Old page blocked page transition
            return;
        }
    }
    this._selectedIndex = index;
    this._updateStatus();
    if (oldPage != null && oldPage !== page) {
        let dim = MochiKit.Style.getElementDimensions(this);
        let offset = (oldIndex < index) ? dim.w : -dim.w;
        MochiKit.Style.setElementPosition(page, { x: offset });
        page._handleEnter({ validateReset: true });
        let cleanup = function () {
            oldPage.hide();
            MochiKit.Style.setElementPosition(oldPage, { x: 0 });
        };
        let opts = { duration: 0.5, x: -offset, afterFinish: cleanup };
        MochiKit.Visual.Move(oldPage, opts);
        MochiKit.Visual.Move(page, opts);
    } else {
        page._handleEnter({ validateReset: true });
    }
    let detail = { index: index, page: page };
    this.emit("change", { detail: detail });
};

/**
 * Cancels the active page operation. This method will also reset the page
 * status of the currently active page to `ANY`. This method is triggered when
 * the user presses the "Cancel" button.
 *
 * @see RapidContext.Widget.Pane.ANY
 */
RapidContext.Widget.Wizard.prototype.cancel = function () {
    let page = this.activePage();
    page.setAttrs({ pageStatus: RapidContext.Widget.Pane.ANY });
    this.emit("cancel");
};

/**
 * Moves the wizard backward to the previous page and sends the `onchange`
 * signal. This method is triggered when the user presses the "Previous"
 * button.
 */
RapidContext.Widget.Wizard.prototype.previous = function () {
    if (this._selectedIndex > 0) {
        this.activatePage(this._selectedIndex - 1);
    }
};

/**
 * Moves the wizard forward to the next page and sends the `onchange` signal.
 * The page will not be changed if the active page fails a validation check.
 * This method is triggered when the user presses the "Next" button.
 */
RapidContext.Widget.Wizard.prototype.next = function () {
    if (this._selectedIndex < this.getChildNodes().length - 1) {
        this.activatePage(this._selectedIndex + 1);
    }
};

/**
 * Sends the wizard `onclose` signal. This method is triggered when the user
 * presses the "Finish" button.
 */
RapidContext.Widget.Wizard.prototype.done = function () {
    let page = this.activePage();
    if (page != null) {
        if (!page._handleExit({ validate: true })) {
            // Page blocked wizard completion
            return;
        }
    }
    this.emit("close");
};
