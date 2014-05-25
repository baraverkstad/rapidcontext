/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2014 Per Cederberg. All rights reserved.
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
 * @param {String} [attrs.class] the icon CSS class names
 * @param {String} [attrs.url] the icon image URL
 * @param {String} [attrs.position] the icon image position
 * @param {Number} [attrs.width] the icon width (in pixels)
 * @param {Number} [attrs.height] the icon height (in pixels)
 * @param {String} [attrs.tooltip] the icon tooltip text
 * @param {Boolean} [attrs.disabled] the disabled widget flag, defaults to
 *            false
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The icon widget class. Used to provide a small clickable image, using
 *     the `<i>` HTML element. The icons can be either image- or font-based,
 *     causing slightly different behaviors with respect to size. A number of
 *     predefined icons suitable for different actions are available (using the
 *     Font-Awesome icon font).
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var style = { "margin-left": "3px" };
 * var dataReload = RapidContext.Widget.Icon({ ref: "RELOAD", style: style });
 * var dataLoading = RapidContext.Widget.Icon({ ref: "LOADING", hidden: true, style: style });
 * var h3 = MochiKit.DOM.H3({}, "Data List:", dataReload, dataLoading);
 *
 * @example {User Interface XML}
 * <h3>
 *   Data List:
 *   <Icon id="dataReload" ref="RELOAD" style="margin-left: 3px;" />
 *   <Icon id="dataLoading" ref="LOADING" hidden="true" style="margin-left: 3px;" />
 * </h3>
 */
RapidContext.Widget.Icon = function (attrs) {
    var o = MochiKit.DOM.createDOM('i');
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.setAttrs(attrs);
    o.addClass("widgetIcon");
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Icon = RapidContext.Widget.Icon;

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} returns null, since child nodes are not supported
 */
RapidContext.Widget.Icon.prototype._containerNode = function () {
    return null;
};

/**
 * Emitted when the icon has been clicked. This is a standard DOM
 * event.
 *
 * @name RapidContext.Widget.Icon#onclick
 * @event
 */

/**
 * Updates the icon or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.ref] the predefined icon name (reference)
 * @param {String} [attrs.class] the icon CSS class names
 * @param {String} [attrs.url] the icon image URL
 * @param {String} [attrs.position] the icon image position
 * @param {Number} [attrs.width] the icon width (in pixels)
 * @param {Number} [attrs.height] the icon height (in pixels)
 * @param {String} [attrs.tooltip] the icon tooltip text
 * @param {Boolean} [attrs.disabled] the disabled widget flag
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Icon.prototype.setAttrs = function (attrs) {
    var locals = MochiKit.Base.update({}, attrs);
    while (locals.ref) {
        var ref = RapidContext.Widget.Icon[locals.ref] || {};
        delete locals.ref;
        MochiKit.Base.setdefault(locals, ref);
    }
    var styles = {};
    if (locals.url) {
        this.addClass("widgetIconSprite");
        styles.backgroundImage = 'url("' + locals.url + '")';
        delete locals.url;
    }
    if (locals.position) {
        styles.backgroundPosition = locals.position;
        delete locals.position;
    }
    if (locals.width) {
        styles.width = locals.width + "px";
        delete locals.width;
    }
    if (locals.height) {
        styles.height = locals.height + "px";
        delete locals.height;
    }
    this.setStyle(styles);
    if (locals.tooltip && !locals.title) {
        locals.title = locals.tooltip;
        delete locals.tooltip;
    }
    this.__setAttrs(locals);
};

/**
 * @scope RapidContext.Widget.Icon.prototype
 */
MochiKit.Base.update(RapidContext.Widget.Icon, {
    /** The default icon definition, inherited by all others. */
    DEFAULT: { url: "rapidcontext/files/images/icons/icons_16x16.png", width: 16, height: 16 },
    /** The blank icon definition. */
    BLANK: { ref: "DEFAULT", position: "0px 0px", style: { cursor: "default" } },
    /** The loading icon definition. */
    LOADING: { ref: "DEFAULT", url: "rapidcontext/files/images/icons/loading.gif", position: "0px 0px", tooltip: "Loading..." },
    /** The close icon definition. */
    CLOSE: { ref: "DEFAULT", position: "0px -16px", tooltip: "Close" },
    /** The close (inverse video) icon definition. */
    CLOSE_INVERSE: { ref: "DEFAULT", position: "0px -32px", tooltip: "Close" },
    /** The close active icon definition. */
    CLOSE_ACTIVE: { ref: "DEFAULT", position: "0px -48px", tooltip: "Close" },
    /** The bluewhite close icon definition. */
    CLOSE_BLUEWHITE: { ref: "DEFAULT", position: "0px -64px", tooltip: "Close" },
    /** The blue close icon definition. */
    CLOSE_BLUE: { ref: "DEFAULT", position: "0px -80px", tooltip: "Close" },
    /** The resize icon definition. */
    RESIZE: { ref: "DEFAULT", position: "0px -96px", style: { cursor: "se-resize" } },
    /** The ok icon definition. */
    OK: { ref: "DEFAULT", position: "-16px 0px", tooltip: "OK" },
    /** The stop icon definition. */
    STOP: { ref: "DEFAULT", position: "-16px -16px", tooltip: "Stop" },
    /** The yes icon definition. */
    YES: { ref: "DEFAULT", position: "-16px -32px", tooltip: "Yes" },
    /** The no icon definition. */
    NO: { ref: "DEFAULT", position: "-16px -48px", tooltip: "No" },
    /** The cancel icon definition. */
    CANCEL: { ref: "DEFAULT", position: "-16px -64px", tooltip: "Cancel" },
    /** The up icon definition. */
    UP: { ref: "DEFAULT", position: "-32px -0px", tooltip: "Move up" },
    /** The down icon definition. */
    DOWN: { ref: "DEFAULT", position: "-32px -16px", tooltip: "Move down" },
    /** The left icon definition. */
    LEFT: { ref: "DEFAULT", position: "-32px -32px", tooltip: "Move left" },
    /** The right icon definition. */
    RIGHT: { ref: "DEFAULT", position: "-32px -48px", tooltip: "Move right" },
    /** The first icon definition. */
    FIRST: { ref: "DEFAULT", position: "-32px -64px", tooltip: "First" },
    /** The last icon definition. */
    LAST: { ref: "DEFAULT", position: "-32px -80px", tooltip: "Last" },
    /** The previous icon definition. */
    PREVIOUS: { ref: "DEFAULT", position: "-32px -96px", tooltip: "Previous" },
    /** The next icon definition. */
    NEXT: { ref: "DEFAULT", position: "-32px -112px", tooltip: "Next" },
    /** The plus icon definition. */
    PLUS: { ref: "DEFAULT", position: "-32px -128px", tooltip: "Show" },
    /** The minus icon definition. */
    MINUS: { ref: "DEFAULT", position: "-32px -144px", tooltip: "Hide" },
    /** The remove icon definition. */
    REMOVE: { ref: "DEFAULT", position: "-48px 0px", tooltip: "Remove" },
    /** The add icon definition. */
    ADD: { ref: "DEFAULT", position: "-48px -16px", tooltip: "Add" },
    /** The copy icon definition. */
    COPY: { ref: "DEFAULT", position: "-48px -32px", tooltip: "Copy" },
    /** The cut icon definition. */
    CUT: { ref: "DEFAULT", position: "-48px -48px", tooltip: "Cut" },
    /** The delete icon definition. */
    DELETE: { ref: "DEFAULT", position: "-48px -64px", tooltip: "Delete" },
    /** The reload icon definition. */
    RELOAD: { ref: "DEFAULT", position: "-48px -80px", tooltip: "Reload" },
    /** The edit icon definition. */
    EDIT: { ref: "DEFAULT", position: "-48px -96px", tooltip: "Edit" },
    /** The white edit icon definition. */
    EDIT_WHITE: { ref: "DEFAULT", position: "-48px -112px", tooltip: "Edit" },
    /** The layout edit icon definition. */
    EDIT_LAYOUT: { ref: "DEFAULT", position: "-48px -128px", tooltip: "Edit" },
    /** The search icon definition. */
    SEARCH: { ref: "DEFAULT", position: "-48px -144px", tooltip: "Search" },
    /** The expand icon definition. */
    EXPAND: { ref: "DEFAULT", position: "-48px -160px", tooltip: "Open in new window" },
    /** The asterisk icon definition. */
    ASTERISK: { ref: "DEFAULT", position: "-64px 0px", tooltip: "Mark" },
    /** The select icon definition. */
    SELECT: { ref: "DEFAULT", position: "-64px -16px", tooltip: "Select / Unselect" },
    /** The like icon definition. */
    LIKE: { ref: "DEFAULT", position: "-64px -32px", tooltip: "Like / Unlike" },
    /** The red flag icon definition. */
    FLAG_RED: { ref: "DEFAULT", position: "-64px -48px", tooltip: "Flag" },
    /** The blue flag icon definition. */
    FLAG_BLUE: { ref: "DEFAULT", position: "-64px -64px", tooltip: "Flag" },
    /** The green flag icon definition. */
    FLAG_GREEN: { ref: "DEFAULT", position: "-64px -80px", tooltip: "Flag" },
    /** The yellow flag icon definition. */
    FLAG_YELLOW: { ref: "DEFAULT", position: "-64px -96px", tooltip: "Flag" },
    /** The red tag icon definition. */
    TAG_RED: { ref: "DEFAULT", position: "-64px -112px", tooltip: "Tag" },
    /** The blue tag icon definition. */
    TAG_BLUE: { ref: "DEFAULT", position: "-64px -128px", tooltip: "Tag" },
    /** The green tag icon definition. */
    TAG_GREEN: { ref: "DEFAULT", position: "-64px -144px", tooltip: "Tag" },
    /** The yellow tag icon definition. */
    TAG_YELLOW: { ref: "DEFAULT", position: "-64px -160px", tooltip: "Tag" },
    /** The options icon definition. */
    OPTIONS: { ref: "DEFAULT", position: "-80px 0px", tooltip: "Options" },
    /** The configure icon definition. */
    CONFIG: { ref: "DEFAULT", position: "-80px -16px", tooltip: "Configure" },
    /** The attach file icon definition. */
    ATTACH: { ref: "DEFAULT", position: "-80px -32px", tooltip: "Attach file" },
    /** The automatic icon definition. */
    AUTOMATIC: { ref: "DEFAULT", position: "-80px -48px", tooltip: "Automatic actions" },
    /** The export icon definition. */
    EXPORT: { ref: "DEFAULT", position: "-80px -64px", tooltip: "Export" },
    /** The information icon definition. */
    INFO: { ref: "DEFAULT", position: "-96px 0px", tooltip: "Information" },
    /** The help icon definition. */
    HELP: { ref: "DEFAULT", position: "-96px -16px", tooltip: "Help" },
    /** The warning icon definition. */
    WARNING: { ref: "DEFAULT", position: "-96px -32px", tooltip: "Warning" },
    /** The error icon definition. */
    ERROR: { ref: "DEFAULT", position: "-96px -48px", tooltip: "Error" },
    /** The bar chart icon definition. */
    BARCHART: { ref: "DEFAULT", position: "-112px 0px", tooltip: "Bar chart" },
    /** The line chart icon definition. */
    LINECHART: { ref: "DEFAULT", position: "-112px -16px", tooltip: "Line chart" },
    /** The curve chart icon definition. */
    CURVECHART: { ref: "DEFAULT", position: "-112px -32px", tooltip: "Curve chart" },
    /** The pie chart icon definition. */
    PIECHART: { ref: "DEFAULT", position: "-112px -48px", tooltip: "Pie chart" },
    /** The organization chart icon definition. */
    ORGCHART: { ref: "DEFAULT", position: "-112px -64px", tooltip: "Organization chart" },
    /** The web feed icon definition. */
    FEED: { ref: "DEFAULT", position: "-128px 0px", tooltip: "RSS/Atom feed" },
    /** The RSS icon definition. */
    RSS: { ref: "DEFAULT", position: "-128px -16px", tooltip: "RSS feed" },
    /** The CSS icon definition. */
    CSS: { ref: "DEFAULT", position: "-128px -32px", tooltip: "CSS" },
    /** The HTML icon definition. */
    HTML: { ref: "DEFAULT", position: "-128px -48px", tooltip: "HTML" },
    /** The XHTML icon definition. */
    XHTML: { ref: "DEFAULT", position: "-128px -64px", tooltip: "XHTML" },
    /** The font icon definition. */
    FONT: { ref: "DEFAULT", position: "-144px 0px", tooltip: "Font" },
    /** The style icon definition. */
    STYLE: { ref: "DEFAULT", position: "-144px -16px", tooltip: "Style" },
    /** The text format icon definition. */
    TEXT_FORMAT: { ref: "DEFAULT", position: "-144px -32px", tooltip: "Text format" },
    /** The calendar icon definition. */
    CALENDAR: { ref: "DEFAULT", position: "-144px -48px", tooltip: "Calendar" },
    /** The date icon definition. */
    DATE: { ref: "DEFAULT", position: "-144px -64px", tooltip: "Date" },
    /** The layout icon definition. */
    LAYOUT: { ref: "DEFAULT", position: "-144px -80px", tooltip: "Layout" },
    /** The table icon definition. */
    TABLE: { ref: "DEFAULT", position: "-144px -96px", tooltip: "Table" },
    /** The sum icon definition. */
    SUM: { ref: "DEFAULT", position: "-144px -112px", tooltip: "Sum" },
    /** The vector icon definition. */
    VECTOR: { ref: "DEFAULT", position: "-144px -128px", tooltip: "Vectors" },
    /** The color icon definition. */
    COLOR: { ref: "DEFAULT", position: "-144px -144px", tooltip: "Color" },
    /** The HTML source icon definition. */
    HTML_SOURCE: { ref: "DEFAULT", position: "-144px -160px", tooltip: "HTML source" },
    /** The plug-in icon definition. */
    PLUGIN: { ref: "DEFAULT", position: "-160px 0px", tooltip: "Plug-in" },
    /** The add plug-in icon definition. */
    PLUGIN_ADD: { ref: "DEFAULT", position: "-160px -16px", tooltip: "Add plug-in" },
    /** The remove plug-in icon definition. */
    PLUGIN_REMOVE: { ref: "DEFAULT", position: "-160px -32px", tooltip: "Remove plug-in" },
    /** The inactive plug-in icon definition. */
    PLUGIN_INACTIVE: { ref: "DEFAULT", position: "-160px -48px", tooltip: "Inactive plug-in" },
    /** The plug-in error icon definition. */
    PLUGIN_ERROR: { ref: "DEFAULT", position: "-160px -64px", tooltip: "Plug-in error" },
    /** The user icon definition. */
    USER: { ref: "DEFAULT", position: "-160px -80px", tooltip: "User" },
    /** The group icon definition. */
    GROUP: { ref: "DEFAULT", position: "-160px -96px", tooltip: "Group" },
    /** The folder icon definition. */
    FOLDER: { ref: "DEFAULT", position: "-176px 0px", tooltip: "Folder" },
    /** The add folder icon definition. */
    FOLDER_ADD: { ref: "DEFAULT", position: "-176px -16px", tooltip: "Add folder" },
    /** The lock icon definition. */
    LOCK: { ref: "DEFAULT", position: "-176px -32px", tooltip: "Lock" },
    /** The key icon definition. */
    KEY: { ref: "DEFAULT", position: "-176px -48px", tooltip: "Key" },
    /** The document icon definition. */
    DOCUMENT: { ref: "DEFAULT", position: "-192px 0px", tooltip: "Document" },
    /** The Word document icon definition. */
    DOCUMENT_WORD: { ref: "DEFAULT", position: "-192px -16px", tooltip: "Word document" },
    /** The Excel document icon definition. */
    DOCUMENT_EXCEL: { ref: "DEFAULT", position: "-192px -32px", tooltip: "Excel document" },
    /** The Office document icon definition. */
    DOCUMENT_OFFICE: { ref: "DEFAULT", position: "-192px -48px", tooltip: "Office document" },
    /** The PDF document icon definition. */
    DOCUMENT_PDF: { ref: "DEFAULT", position: "-192px -64px", tooltip: "PDF document" },
    /** The document search icon definition. */
    DOCUMENT_SEARCH: { ref: "DEFAULT", position: "-192px -80px", tooltip: "Search document" },
    /** The code document icon definition. */
    DOCUMENT_CODE: { ref: "DEFAULT", position: "-192px -96px", tooltip: "Code document" },
    /** The text document icon definition. */
    DOCUMENT_TEXT: { ref: "DEFAULT", position: "-192px -112px", tooltip: "Text document" },
    /** The contact icon definition. */
    CONTACT: { ref: "DEFAULT", position: "-208px 0px", tooltip: "Contact" },
    /** The phone icon definition. */
    PHONE: { ref: "DEFAULT", position: "-208px -16px", tooltip: "Phone number" },
    /** The mobile phone icon definition. */
    PHONE_MOBILE: { ref: "DEFAULT", position: "-208px -32px", tooltip: "Mobile phone number" },
    /** The comment icon definition. */
    COMMENT: { ref: "DEFAULT", position: "-208px -48px", tooltip: "Comment" },
    /** The comments icon definition. */
    COMMENTS: { ref: "DEFAULT", position: "-208px -64px", tooltip: "Comments" },
    /** The note icon definition. */
    NOTE: { ref: "DEFAULT", position: "-208px -80px", tooltip: "Note" },
    /** The application icon definition. */
    APPLICATION: { ref: "DEFAULT", position: "-224px 0px", tooltip: "Application" },
    /** The application terminal icon definition. */
    APPLICATION_TERMINAL: { ref: "DEFAULT", position: "-224px -16px", tooltip: "Terminal" },
    /** The dialog icon definition. */
    DIALOG: { ref: "DEFAULT", position: "-224px -32px", tooltip: "Dialog" },
    /** The script icon definition. */
    SCRIPT: { ref: "DEFAULT", position: "-224px -48px", tooltip: "Script" },
    /** The component icon definition. */
    COMPONENT: { ref: "DEFAULT", position: "-224px -64px", tooltip: "Component" },
    /** The components icon definition. */
    COMPONENTS: { ref: "DEFAULT", position: "-224px -80px", tooltip: "Components" },
    /** The package icon definition. */
    PACKAGE: { ref: "DEFAULT", position: "-224px -96px", tooltip: "Package" },
    /** The textfield icon definition. */
    TEXTFIELD: { ref: "DEFAULT", position: "-224px -112px", tooltip: "Text field" },
    /** The network drive icon definition. */
    DRIVE_NETWORK: { ref: "DEFAULT", position: "-240px 0px", tooltip: "Network drive" },
    /** The monitor icon definition. */
    MONITOR: { ref: "DEFAULT", position: "-240px -16px", tooltip: "Monitor" },
    /** The keyboard icon definition. */
    KEYBOARD: { ref: "DEFAULT", position: "-240px -32px", tooltip: "Keyboard" },
    /** The printer icon definition. */
    PRINTER: { ref: "DEFAULT", position: "-240px -48px", tooltip: "Printer" },
    /** The server icon definition. */
    SERVER: { ref: "DEFAULT", position: "-240px -64px", tooltip: "Server" },
    /** The disk icon definition. */
    DISK: { ref: "DEFAULT", position: "-240px -80px", tooltip: "Disk" },
    /** The database icon definition. */
    DATABASE: { ref: "DEFAULT", position: "-240px -96px", tooltip: "Database" },
    /** The email icon definition. */
    EMAIL: { ref: "DEFAULT", position: "-240px -112px", tooltip: "Email" },
    /** The image icon definition. */
    IMAGE: { ref: "DEFAULT", position: "-256px 0px", tooltip: "Image" },
    /** The calculator icon definition. */
    CALCULATOR: { ref: "DEFAULT", position: "-256px -16px", tooltip: "Calculator" },
    /** The home icon definition. */
    HOME: { ref: "DEFAULT", position: "-256px -32px", tooltip: "Home" },
    /** The book icon definition. */
    BOOK: { ref: "DEFAULT", position: "-256px -48px", tooltip: "Book" },
    /** The open book icon definition. */
    BOOK_OPEN: { ref: "DEFAULT", position: "-256px -64px", tooltip: "Book" },
    /** The clock icon definition. */
    CLOCK: { ref: "DEFAULT", position: "-256px -80px", tooltip: "Clock" },
    /** The delay icon definition. */
    DELAY: { ref: "DEFAULT", position: "-256px -96px", tooltip: "Delay" }
});
