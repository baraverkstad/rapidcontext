/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new wizard widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Widget} [...] the child widgets or DOM nodes (should be
 *            Pane widgets)
 *
 * @return {Widget} the widget DOM node
 *
 * @class The wizard widget class. Used to provide a sequence of
 *     pages, where the user can step forward and backward with
 *     buttons. Internally it uses a &lt;div&gt; HTML element
 *     containing Pane widgets that are hidden and shown according
 *     to the page transitions. In addition to standard HTML events,
 *     the "onchange" event is triggered on page transitions, the
 *     "oncancel" event is triggered if a user cancels an operation,
 *     and the "onclose" event is triggered when the wizard
 *     completes.
 * @extends RapidContext.Widget
 *
 * @example
 * &lt;Dialog id="exDialog" title="Example Dialog" w="80%" h="50%"&gt;
 *   &lt;Wizard id="exWizard" style="width: 100%; height: 100%;"&gt;
 *     &lt;Pane pageTitle="The first step"&gt;
 *       ...
 *     &lt;/Pane&gt;
 *     &lt;Pane pageTitle="The second step"&gt;
 *       ...
 *     &lt;/Pane&gt;
 *   &lt;/Wizard&gt;
 * &lt;/Dialog&gt;
 */
RapidContext.Widget.Wizard = function (attrs/*, ... */) {
    var o = MochiKit.DOM.DIV(attrs);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetWizard");
    o._selectedIndex = -1;
    o.appendChild(MochiKit.DOM.H3({ "class": "widgetWizardTitle" }));
    var bCancel = RapidContext.Widget.Button({ icon:"CANCEL", style: { "margin-right": "10px" }},
                                             "Cancel");
    var bPrev = RapidContext.Widget.Button({ icon: "PREVIOUS", style: { "margin-right": "10px" }},
                                           "Previous");
    var bNext = RapidContext.Widget.Button({},
                                           "Next",
                                           RapidContext.Widget.Icon({ ref: "NEXT", style: { margin: "0 0 0 6px" }}));
    var bDone = RapidContext.Widget.Button({ icon: "OK", highlight: true },
                                           "Finish");
    bCancel.hide();
    o.appendChild(MochiKit.DOM.DIV({ "class": "widgetWizardButtons" },
                                   bCancel, bPrev, bNext, bDone));
    MochiKit.Signal.connect(bCancel, "onclick", o, "cancel");
    MochiKit.Signal.connect(bPrev, "onclick", o, "previous");
    MochiKit.Signal.connect(bNext, "onclick", o, "next");
    MochiKit.Signal.connect(bDone, "onclick", o, "done");
    o._updateStatus();
    o.setAttrs(attrs);
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Wizard = RapidContext.Widget.Wizard;

/**
 * Returns an array with all child pane widgets. Note that the array
 * is a real JavaScript array, not a dynamic NodeList.
 *
 * @return {Array} the array of child wizard page widgets
 */
RapidContext.Widget.Wizard.prototype.getChildNodes = function () {
    return MochiKit.Base.extend([], this.childNodes, 2);
};

/**
 * Adds a single child page widget to this widget. The child widget
 * should be a RapidContext.Widget.Pane widget, or it will be added to a
 * new one.
 *
 * @param {Widget} child the page widget to add
 */
RapidContext.Widget.Wizard.prototype.addChildNode = function (child) {
    if (!RapidContext.Widget.isWidget(child, "Pane")) {
        child = RapidContext.Widget.Pane(null, child);
    }
    RapidContext.Util.registerSizeConstraints(child, "100%", "100%-65");
    child.hide();
    this.appendChild(child);
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
    var h3 = this.childNodes[0];
    var bCancel = this.childNodes[1].childNodes[0];
    var bPrev = this.childNodes[1].childNodes[1];
    var bNext = this.childNodes[1].childNodes[2];
    var bDone = this.childNodes[1].childNodes[3];
    var page = this.activePage();
    var status = RapidContext.Widget.Pane.FORWARD;
    var title = null;
    var info = "(No pages available)";
    var icon = null;
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
    bPrev.disabled = (this._selectedIndex <= 0) || !status.previous;
    bNext.disabled = !status.next;
    bDone.disabled = !status.next;
    info = MochiKit.DOM.SPAN({ "class": "widgetWizardInfo" }, info);
    MochiKit.DOM.replaceChildNodes(h3, icon, title, info);
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
 * Activates a new page.
 *
 * @param {Number/Widget} indexOrPage the page index or page DOM node
 */
RapidContext.Widget.Wizard.prototype.activatePage = function (indexOrPage) {
    if (typeof(indexOrPage) == "number") {
        var index = indexOrPage;
        var page = this.childNodes[index + 2];
    } else {
        var page = indexOrPage;
        var index = MochiKit.Base.findIdentical(this.childNodes, page, 2) - 2;
    }
    if (index < 0 || index >= this.getChildNodes().length) {
        throw new RangeError("Page index out of bounds: " + index);
    }
    var oldIndex = this._selectedIndex;
    var oldPage = this.activePage();
    if (oldPage != null && oldPage !== page) {
        if (!oldPage._handleExit({ hide: false, validate: this._selectedIndex < index })) {
            // Old page blocked page transition
            return;
        }
    }
    this._selectedIndex = index;
    this._updateStatus();
    if (oldPage != null && oldPage !== page) {
        var dim = MochiKit.Style.getElementDimensions(this);
        var offset = (oldIndex < index) ? dim.w : -dim.w;
        MochiKit.Style.setElementPosition(page, { x: offset });
        page._handleEnter({ validateReset: true });
        var cleanup = function () {
            oldPage.hide();
            MochiKit.Style.setElementPosition(oldPage, { x: 0 });
        };
        var opts = { duration: 0.5, x: -offset, afterFinish: cleanup };
        MochiKit.Visual.Move(oldPage, opts);
        MochiKit.Visual.Move(page, opts);
    } else {
        page._handleEnter({ validateReset: true });
    }
    RapidContext.Widget.emitSignal(this, "onchange", index, page);
};

/**
 * Cancels the active page operation. This method will also reset
 * the page status of the currently active page to "ANY".
 */
RapidContext.Widget.Wizard.prototype.cancel = function () {
    var page = this.activePage();
    page.setAttrs({ pageStatus: RapidContext.Widget.Pane.ANY });
    RapidContext.Widget.emitSignal(this, "oncancel");
};

/**
 * Moves the wizard backward to the previous page.
 */
RapidContext.Widget.Wizard.prototype.previous = function () {
    if (this._selectedIndex > 0) {
        this.activatePage(this._selectedIndex - 1);
    }
};

/**
 * Moves the wizard forward to the next page. The page will not be
 * changed if the active page fails a validation check.
 */
RapidContext.Widget.Wizard.prototype.next = function () {
    if (this._selectedIndex < this.getChildNodes().length - 1) {
        this.activatePage(this._selectedIndex + 1);
    }
};

/**
 * Sends the wizard onclose signal when the user presses the finish
 * button.
 */
RapidContext.Widget.Wizard.prototype.done = function () {
    var page = this.activePage();
    if (page != null) {
        if (!page._handleExit({ validate: true })) {
            // Page blocked wizard completion
            return;
        }
    }
    RapidContext.Widget.emitSignal(this, "onclose");
};

/**
 * Resizes the current wizard page. This method need not be called
 * directly, but is automatically called whenever a parent node is
 * resized. It optimizes the resize chain by only resizing those DOM
 * child nodes that are visible.
 */
RapidContext.Widget.Wizard.prototype.resizeContent = function () {
    var page = this.activePage();
    if (page != null) {
        RapidContext.Util.resizeElements(page);
    }
};
