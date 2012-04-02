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
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new icon widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} [attrs.ref] the referenced icon definition
 * @param {String} [attrs.src] the icon image source URL (unmodified)
 * @param {String} [attrs.url] the icon image file URL, prepended by
 *             the "baseUrl" (that is inherited from the default icon)
 * @param {String} [attrs.baseUrl] the icon image base URL, used only
 *             to prepend to "url" (normally only specified in the
 *             default icon)
 * @param {String} [attrs.tooltip] the icon tooltip text
 *
 * @return {Widget} the widget DOM node
 *
 * @class The icon widget class. Used to provide a small clickable
 *     image, using the &lt;img&gt; HTML element. In particular,
 *     the "onclick" event is usually of interest. Predefined icon
 *     images for variuos purposes are available as constants.
 * @extends RapidContext.Widget
 */
RapidContext.Widget.Icon = function (attrs) {
    var o = MochiKit.DOM.IMG();
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.setAttrs(attrs);
    o.addClass("widgetIcon");
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Icon = RapidContext.Widget.Icon;

/**
 * Updates the icon or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.ref] the referenced icon definition
 * @param {String} [attrs.src] the icon image source URL (unmodified)
 * @param {String} [attrs.url] the icon image file URL, prepended by
 *             the "baseUrl" (that is inherited from the default icon)
 * @param {String} [attrs.baseUrl] the icon image base URL, used only
 *             to prepend to "url" (normally only specified in the
 *             default icon)
 * @param {String} [attrs.tooltip] the icon tooltip text
 */
RapidContext.Widget.Icon.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    if (attrs.ref) {
        MochiKit.Base.setdefault(attrs,
                                 RapidContext.Widget.Icon[attrs.ref],
                                 RapidContext.Widget.Icon.DEFAULT);
    }
    var locals = RapidContext.Util.mask(attrs, ["ref", "url", "baseUrl", "tooltip", "width", "height"]);
    if (typeof(locals.url) != "undefined") {
        MochiKit.Base.setdefault(locals, RapidContext.Widget.Icon.DEFAULT);
        attrs.src = locals.baseUrl + locals.url;
    }
    if (typeof(locals.tooltip) != "undefined") {
        attrs.alt = locals.tooltip;
        attrs.title = locals.tooltip;
    }
    /* TODO: Fix width and height for IE, as it seems that the
             values set by setAttribute() are ignored. */
    if (typeof(locals.width) != "undefined") {
        this.width = locals.width;
        this.setStyle({ width: locals.width });
    }
    if (typeof(locals.height) != "undefined") {
        this.height = locals.height;
        this.setStyle({ height: locals.height });
    }
    MochiKit.DOM.updateNodeAttributes(this, attrs);
};

/**
 * @scope RapidContext.Widget.Icon.prototype
 */
MochiKit.Base.update(RapidContext.Widget.Icon, {
    /** The default icon definition, inherited by all others. */
    DEFAULT: { baseUrl: "images/icons/", width: "16px", height: "16px" },
    /** The blank icon definition. */
    BLANK: { url: "blank.gif", style: { cursor: "default" } },
    /** The close icon definition. */
    CLOSE: { url: "close.gif" },
    /** The resize icon definition. */
    RESIZE: { url: "resize-handle.gif", style: { cursor: "se-resize" } },
    /** The ok icon definition. */
    OK: { url: "ok.gif", tooltip: "OK" },
    /** The cancel icon definition. */
    CANCEL: { url: "cancel.gif", tooltip: "Cancel" },
    /** The yes icon definition. */
    YES: { url: "yes.gif", tooltip: "Yes" },
    /** The no icon definition. */
    NO: { url: "no.gif", tooltip: "No" },
    /** The help icon definition. */
    HELP: { url: "help.gif", tooltip: "Help" },
    /** The error icon definition. */
    ERROR: { url: "error.gif", tooltip: "Error" },
    /** The plus icon definition. */
    PLUS: { url: "plus.gif", tooltip: "Show" },
    /** The minus icon definition. */
    MINUS: { url: "minus.gif", tooltip: "Hide" },
    /** The next icon definition. */
    NEXT: { url: "next.gif", tooltip: "Next" },
    /** The previuos icon definition. */
    PREVIOUS: { url: "previous.gif", tooltip: "Previous" },
    /** The config icon definition. */
    CONFIG: { url: "config.gif", tooltip: "Configure" },
    /** The options icon definition. */
    OPTIONS: { url: "options.gif", tooltip: "Options" },
    /** The delay icon definition. */
    DELAY: { url: "delay.gif", tooltip: "Configure Delay" },
    /** The reload icon definition. */
    RELOAD: { url: "reload.gif", tooltip: "Reload" },
    /** The loading icon definition. */
    LOADING: { url: "loading.gif", tooltip: "Loading..." },
    /** The large loading icon definition. */
    LOADING_LARGE: { url: "loading-large.gif", tooltip: "Loading...", width: "32px", height: "32px" },
    /** The search icon definition. */
    SEARCH: { url: "magnifier.gif", tooltip: "Search" },
    /** The add icon definition. */
    ADD: { url: "add.gif", tooltip: "Add" },
    /** The remove icon definition. */
    REMOVE: { url: "remove.gif", tooltip: "Remove" },
    /** The edit icon definition. */
    EDIT: { url: "edit.gif", tooltip: "Edit" },
    /** The delete icon definition. */
    DELETE: { url: "trash.gif", tooltip: "Clear / Delete" },
    /** The select icon definition. */
    SELECT: { url: "select.gif", tooltip: "Select / Unselect" },
    /** The cut icon definition. */
    CUT: { url: "cut.gif", tooltip: "Cut" },
    /** The config icon definition. */
    DIALOG: { url: "dialog.gif", tooltip: "Open Dialog" },
    /** The export icon definition. */
    EXPORT: { url: "export.gif", tooltip: "Export" },
    /** The expand icon definition. */
    EXPAND: { url: "expand.gif", tooltip: "Expand" },
    /** The up icon definition. */
    UP: { url: "up.gif", tooltip: "Move Up" },
    /** The down icon definition. */
    DOWN: { url: "down.gif", tooltip: "Move Down" },
    /** The left icon definition. */
    LEFT: { url: "left.gif", tooltip: "Move Left" },
    /** The right icon definition. */
    RIGHT: { url: "right.gif", tooltip: "Move Right" },
    /** The comment icon definition. */
    COMMENT: { url: "comment.gif", tooltip: "Comment" },
    /** The calendar icon definition. */
    CALENDAR: { url: "calendar.gif", tooltip: "Calendar" },
    /** The automatic icon definition. */
    AUTOMATIC: { url: "automatic.gif", tooltip: "Automatic Processing" },
    /** The plugin icon definition. */
    PLUGIN: { url: "plugin.gif", tooltip: "Plug-in" },
    /** The folder icon definition. */
    FOLDER: { url: "folder.gif" },
    /** The document icon definition. */
    DOCUMENT: { url: "document.gif" },
    /** The bar chart icon definition. */
    BARCHART: { url: "barchart.gif", tooltip: "Bar Chart" },
    /** The line chart icon definition. */
    LINECHART: { url: "linechart.gif", tooltip: "Line Chart" },
    /** The pie chart icon definition. */
    PIECHART: { url: "piechart.gif", tooltip: "Pie Chart" }
});
