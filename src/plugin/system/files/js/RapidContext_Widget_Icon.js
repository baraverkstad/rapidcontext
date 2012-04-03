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
 * Creates a new icon widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.ref] the predefined icon name (reference)
 * @param {String} [attrs.url] the icon image URL
 * @param {String} [attrs.position] the icon position in the image
 * @param {Number} [attrs.width] the icon image width (in pixels)
 * @param {Number} [attrs.height] the icon image height (in pixels)
 * @param {String} [attrs.tooltip] the icon tooltip text
 *
 * @return {Widget} the widget DOM node
 *
 * @class The icon widget class. Used to provide a small clickable
 *     image, using the &lt;span&gt; HTML element. In particular,
 *     the "onclick" event is usually of interest. Predefined icon
 *     images (using a large image sprite) are available.
 * @extends RapidContext.Widget
 */
RapidContext.Widget.Icon = function (attrs) {
    var o = MochiKit.DOM.SPAN();
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
 * @param {String} [attrs.ref] the predefined icon name (reference)
 * @param {String} [attrs.url] the icon image URL
 * @param {String} [attrs.position] the icon position in the image
 * @param {Number} [attrs.width] the icon image width (in pixels)
 * @param {Number} [attrs.height] the icon image height (in pixels)
 * @param {String} [attrs.tooltip] the icon tooltip text
 */
RapidContext.Widget.Icon.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    if (attrs.ref || attrs.url) {
        MochiKit.Base.setdefault(attrs,
                                 RapidContext.Widget.Icon[attrs.ref] || {},
                                 RapidContext.Widget.Icon.DEFAULT);
    }
    var locals = RapidContext.Util.mask(attrs, ["ref", "url", "position", "width", "height", "tooltip"]);
    var styles = {};
    if (locals.url) {
        styles.backgroundImage = 'url("' + locals.url + '")';
    }
    if (locals.position) {
        styles.backgroundPosition = locals.position;
    }
    if (locals.width) {
        styles.width = locals.width + "px";
    }
    if (locals.height != null) {
        styles.height = locals.height + "px";
    }
    this.setStyle(styles);
    if (typeof(locals.tooltip) != "undefined") {
        attrs.title = locals.tooltip;
    }
    MochiKit.DOM.updateNodeAttributes(this, attrs);
};

/**
 * @scope RapidContext.Widget.Icon.prototype
 */
MochiKit.Base.update(RapidContext.Widget.Icon, {
    /** The default icon definition, inherited by all others. */
    DEFAULT: { url: "images/icons/icons_16x16.png", width: 16, height: 16 },
    /** The blank icon definition. */
    BLANK: { position: "0px 0px", style: { cursor: "default" } },
    /** The close icon definition. */
    CLOSE: { url: "images/icons/close.gif" },
    /** The resize icon definition. */
    RESIZE: { url: "images/icons/resize-handle.gif", style: { cursor: "se-resize" } },
    /** The ok icon definition. */
    OK: { position: "-16px 0px", tooltip: "OK" },
    /** The cancel icon definition. */
    CANCEL: { url: "images/icons/cancel.gif", tooltip: "Cancel" },
    /** The yes icon definition. */
    YES: { url: "images/icons/yes.gif", tooltip: "Yes" },
    /** The no icon definition. */
    NO: { url: "images/icons/no.gif", tooltip: "No" },
    /** The help icon definition. */
    HELP: { url: "images/icons/help.gif", tooltip: "Help" },
    /** The error icon definition. */
    ERROR: { position: "-16px -16px", tooltip: "Error" },
    /** The plus icon definition. */
    PLUS: { url: "images/icons/plus.gif", tooltip: "Show" },
    /** The minus icon definition. */
    MINUS: { url: "images/icons/minus.gif", tooltip: "Hide" },
    /** The next icon definition. */
    NEXT: { url: "images/icons/next.gif", tooltip: "Next" },
    /** The previuos icon definition. */
    PREVIOUS: { url: "images/icons/previous.gif", tooltip: "Previous" },
    /** The config icon definition. */
    CONFIG: { url: "images/icons/config.gif", tooltip: "Configure" },
    /** The options icon definition. */
    OPTIONS: { url: "images/icons/options.gif", tooltip: "Options" },
    /** The delay icon definition. */
    DELAY: { url: "images/icons/delay.gif", tooltip: "Configure Delay" },
    /** The reload icon definition. */
    RELOAD: { url: "images/icons/reload.gif", tooltip: "Reload" },
    /** The loading icon definition. */
    LOADING: { url: "images/icons/loading.gif", tooltip: "Loading..." },
    /** The large loading icon definition. */
    LOADING_LARGE: { url: "images/icons/loading-large.gif", tooltip: "Loading...", width: 32, height: 32 },
    /** The search icon definition. */
    SEARCH: { url: "images/icons/magnifier.gif", tooltip: "Search" },
    /** The add icon definition. */
    ADD: { url: "images/icons/add.gif", tooltip: "Add" },
    /** The remove icon definition. */
    REMOVE: { url: "images/icons/remove.gif", tooltip: "Remove" },
    /** The edit icon definition. */
    EDIT: { url: "images/icons/edit.gif", tooltip: "Edit" },
    /** The delete icon definition. */
    DELETE: { url: "images/icons/trash.gif", tooltip: "Clear / Delete" },
    /** The select icon definition. */
    SELECT: { url: "images/icons/select.gif", tooltip: "Select / Unselect" },
    /** The cut icon definition. */
    CUT: { url: "images/icons/cut.gif", tooltip: "Cut" },
    /** The config icon definition. */
    DIALOG: { url: "images/icons/dialog.gif", tooltip: "Open Dialog" },
    /** The export icon definition. */
    EXPORT: { url: "images/icons/export.gif", tooltip: "Export" },
    /** The expand icon definition. */
    EXPAND: { url: "images/icons/expand.gif", tooltip: "Expand" },
    /** The up icon definition. */
    UP: { url: "images/icons/up.gif", tooltip: "Move Up" },
    /** The down icon definition. */
    DOWN: { url: "images/icons/down.gif", tooltip: "Move Down" },
    /** The left icon definition. */
    LEFT: { url: "images/icons/left.gif", tooltip: "Move Left" },
    /** The right icon definition. */
    RIGHT: { url: "images/icons/right.gif", tooltip: "Move Right" },
    /** The comment icon definition. */
    COMMENT: { url: "images/icons/comment.gif", tooltip: "Comment" },
    /** The calendar icon definition. */
    CALENDAR: { url: "images/icons/calendar.gif", tooltip: "Calendar" },
    /** The automatic icon definition. */
    AUTOMATIC: { url: "images/icons/automatic.gif", tooltip: "Automatic Processing" },
    /** The plugin icon definition. */
    PLUGIN: { position: "-32px 0px", tooltip: "Plug-in" },
    /** The plugin icon definition. */
    PLUGIN_INACTIVE: { position: "-32px -48px", tooltip: "Inactive Plug-in" },
    /** The folder icon definition. */
    FOLDER: { url: "images/icons/folder.gif" },
    /** The document icon definition. */
    DOCUMENT: { url: "images/icons/document.gif" },
    /** The bar chart icon definition. */
    BARCHART: { url: "images/icons/barchart.gif", tooltip: "Bar Chart" },
    /** The line chart icon definition. */
    LINECHART: { url: "images/icons/linechart.gif", tooltip: "Line Chart" },
    /** The pie chart icon definition. */
    PIECHART: { url: "images/icons/piechart.gif", tooltip: "Pie Chart" }
});
