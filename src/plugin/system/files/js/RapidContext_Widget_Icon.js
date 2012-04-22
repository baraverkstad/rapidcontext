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
    this.__setAttrs(attrs);
};

/**
 * @scope RapidContext.Widget.Icon.prototype
 */
MochiKit.Base.update(RapidContext.Widget.Icon, {
    /** The default icon definition, inherited by all others. */
    DEFAULT: { url: "images/icons/icons_16x16.png", width: 16, height: 16 },
    /** The blank icon definition. */
    BLANK: { position: "0px 0px", style: { cursor: "default" } },
    /** The loading icon definition. */
    LOADING: { url: "images/icons/loading.gif", position: "0px 0px", tooltip: "Loading..." },
    /** The close icon definition. */
    CLOSE: { position: "0px -16px", tooltip: "Close" },
    /** The close (inverse video) icon definition. */
    CLOSE_INVERSE: { position: "0px -32px", tooltip: "Close" },
    /** The close active icon definition. */
    CLOSE_ACTIVE: { position: "0px -48px", tooltip: "Close" },
    /** The bluewhite close icon definition. */
    CLOSE_BLUEWHITE: { position: "0px -64px", tooltip: "Close" },
    /** The blue close icon definition. */
    CLOSE_BLUE: { position: "0px -80px", tooltip: "Close" },
    /** The resize icon definition. */
    RESIZE: { position: "0px -96px", style: { cursor: "se-resize" } },
    /** The ok icon definition. */
    OK: { position: "-16px 0px", tooltip: "OK" },
    /** The stop icon definition. */
    STOP: { position: "-16px -16px", tooltip: "Stop" },
    /** The yes icon definition. */
    YES: { position: "-16px -32px", tooltip: "Yes" },
    /** The no icon definition. */
    NO: { position: "-16px -48px", tooltip: "No" },
    /** The cancel icon definition. */
    CANCEL: { position: "-16px -64px", tooltip: "Cancel" },
    /** The up icon definition. */
    UP: { position: "-32px -0px", tooltip: "Move Up" },
    /** The down icon definition. */
    DOWN: { position: "-32px -16px", tooltip: "Move Down" },
    /** The left icon definition. */
    LEFT: { position: "-32px -32px", tooltip: "Move Left" },
    /** The right icon definition. */
    RIGHT: { position: "-32px -48px", tooltip: "Move Right" },
    /** The first icon definition. */
    FIRST: { position: "-32px -64px", tooltip: "Move First" },
    /** The last icon definition. */
    LAST: { position: "-32px -80px", tooltip: "Move Last" },
    /** The previous icon definition. */
    PREVIOUS: { position: "-32px -96px", tooltip: "Move Previous" },
    /** The next icon definition. */
    NEXT: { position: "-32px -112px", tooltip: "Move Next" },
    /** The plus icon definition. */
    PLUS: { position: "-32px -128px", tooltip: "Show" },
    /** The minus icon definition. */
    MINUS: { position: "-32px -144px", tooltip: "Hide" },
    /** The remove icon definition. */
    REMOVE: { position: "-48px 0px", tooltip: "Remove" },
    /** The add icon definition. */
    ADD: { position: "-48px -16px", tooltip: "Add" },
    /** The copy icon definition. */
    COPY: { position: "-48px -32px", tooltip: "Copy" },
    /** The cut icon definition. */
    CUT: { position: "-48px -48px", tooltip: "Cut" },
    /** The delete icon definition. */
    DELETE: { position: "-48px -64px", tooltip: "Delete" },
    /** The reload icon definition. */
    RELOAD: { position: "-48px -80px", tooltip: "Reload" },
    /** The edit icon definition. */
    EDIT: { position: "-48px -96px", tooltip: "Edit" },
    /** The white edit icon definition. */
    EDIT_WHITE: { position: "-48px -112px", tooltip: "Edit" },
    /** The layout edit icon definition. */
    EDIT_LAYOUT: { position: "-48px -128px", tooltip: "Edit" },
    /** The search icon definition. */
    SEARCH: { position: "-48px -144px", tooltip: "Search" },
    /** The expand icon definition. */
    EXPAND: { position: "-48px -160px", tooltip: "Open in New Window" },
    /** The asterisk icon definition. */
    ASTERISK: { position: "-64px 0px", tooltip: "Mark" },
    /** The select icon definition. */
    SELECT: { position: "-64px -16px", tooltip: "Select / Unselect" },
    /** The like icon definition. */
    LIKE: { position: "-64px -32px", tooltip: "Like / Unlike" },
    /** The red flag icon definition. */
    FLAG_RED: { position: "-64px -48px", tooltip: "Flag" },
    /** The blue flag icon definition. */
    FLAG_BLUE: { position: "-64px -64px", tooltip: "Flag" },
    /** The green flag icon definition. */
    FLAG_GREEN: { position: "-64px -80px", tooltip: "Flag" },
    /** The yellow flag icon definition. */
    FLAG_YELLOW: { position: "-64px -96px", tooltip: "Flag" },
    /** The red tag icon definition. */
    TAG_RED: { position: "-64px -112px", tooltip: "Tag" },
    /** The blue tag icon definition. */
    TAG_BLUE: { position: "-64px -128px", tooltip: "Tag" },
    /** The green tag icon definition. */
    TAG_GREEN: { position: "-64px -144px", tooltip: "Tag" },
    /** The yellow tag icon definition. */
    TAG_YELLOW: { position: "-64px -160px", tooltip: "Tag" },
    /** The options icon definition. */
    OPTIONS: { position: "-80px 0px", tooltip: "Options" },
    /** The configure icon definition. */
    CONFIG: { position: "-80px -16px", tooltip: "Configure" },
    /** The attach file icon definition. */
    ATTACH: { position: "-80px -32px", tooltip: "Attach File" },
    /** The automatic icon definition. */
    AUTOMATIC: { position: "-80px -48px", tooltip: "Automatic Actions" },
    /** The export icon definition. */
    EXPORT: { position: "-80px -64px", tooltip: "Export" },
    /** The information icon definition. */
    INFO: { position: "-96px 0px", tooltip: "Information" },
    /** The help icon definition. */
    HELP: { position: "-96px -16px", tooltip: "Help" },
    /** The warning icon definition. */
    WARNING: { position: "-96px -32px", tooltip: "Warning" },
    /** The error icon definition. */
    ERROR: { position: "-96px -48px", tooltip: "Error" },
    /** The bar chart icon definition. */
    BARCHART: { position: "-112px 0px", tooltip: "Bar Chart" },
    /** The line chart icon definition. */
    LINECHART: { position: "-112px -16px", tooltip: "Line Chart" },
    /** The curve chart icon definition. */
    CURVECHART: { position: "-112px -32px", tooltip: "Curve Chart" },
    /** The pie chart icon definition. */
    PIECHART: { position: "-112px -48px", tooltip: "Pie Chart" },
    /** The organization chart icon definition. */
    ORGCHART: { position: "-112px -64px", tooltip: "Organization Chart" },
    /** The web feed icon definition. */
    FEED: { position: "-128px 0px", tooltip: "RSS/Atom Feed" },
    /** The RSS icon definition. */
    RSS: { position: "-128px -16px", tooltip: "RSS Feed" },
    /** The CSS icon definition. */
    CSS: { position: "-128px -32px", tooltip: "CSS" },
    /** The HTML icon definition. */
    HTML: { position: "-128px -48px", tooltip: "HTML" },
    /** The XHTML icon definition. */
    XHTML: { position: "-128px -64px", tooltip: "XHTML" },
    /** The font icon definition. */
    FONT: { position: "-144px 0px", tooltip: "Font" },
    /** The style icon definition. */
    STYLE: { position: "-144px -16px", tooltip: "Style" },
    /** The text format icon definition. */
    TEXT_FORMAT: { position: "-144px -32px", tooltip: "Text Format" },
    /** The calendar icon definition. */
    CALENDAR: { position: "-144px -48px", tooltip: "Calendar" },
    /** The date icon definition. */
    DATE: { position: "-144px -64px", tooltip: "Date" },
    /** The layout icon definition. */
    LAYOUT: { position: "-144px -80px", tooltip: "Layout" },
    /** The table icon definition. */
    TABLE: { position: "-144px -96px", tooltip: "Table" },
    /** The sum icon definition. */
    SUM: { position: "-144px -112px", tooltip: "Sum" },
    /** The vector icon definition. */
    VECTOR: { position: "-144px -128px", tooltip: "Vectors" },
    /** The color icon definition. */
    COLOR: { position: "-144px -144px", tooltip: "Color" },
    /** The HTML source icon definition. */
    HTML_SOURCE: { position: "-144px -160px", tooltip: "HTML Source" },
    /** The plug-in icon definition. */
    PLUGIN: { position: "-160px 0px", tooltip: "Plug-in" },
    /** The add plug-in icon definition. */
    PLUGIN_ADD: { position: "-160px -16px", tooltip: "Add Plug-in" },
    /** The remove plug-in icon definition. */
    PLUGIN_REMOVE: { position: "-160px -32px", tooltip: "Remove Plug-in" },
    /** The inactive plug-in icon definition. */
    PLUGIN_INACTIVE: { position: "-160px -48px", tooltip: "Inactive Plug-in" },
    /** The plug-in error icon definition. */
    PLUGIN_ERROR: { position: "-160px -64px", tooltip: "Plug-in Error" },
    /** The user icon definition. */
    USER: { position: "-160px -80px", tooltip: "User" },
    /** The group icon definition. */
    GROUP: { position: "-160px -96px", tooltip: "Group" },
    /** The folder icon definition. */
    FOLDER: { position: "-176px 0px", tooltip: "Folder" },
    /** The add folder icon definition. */
    FOLDER_ADD: { position: "-176px -16px", tooltip: "Add Folder" },
    /** The lock icon definition. */
    LOCK: { position: "-176px -32px", tooltip: "Lock" },
    /** The key icon definition. */
    KEY: { position: "-176px -48px", tooltip: "Key" },
    /** The document icon definition. */
    DOCUMENT: { position: "-192px 0px", tooltip: "Document" },
    /** The Word document icon definition. */
    DOCUMENT_WORD: { position: "-192px -16px", tooltip: "Word Document" },
    /** The Excel document icon definition. */
    DOCUMENT_EXCEL: { position: "-192px -32px", tooltip: "Excel Document" },
    /** The Office document icon definition. */
    DOCUMENT_OFFICE: { position: "-192px -48px", tooltip: "Office Document" },
    /** The PDF document icon definition. */
    DOCUMENT_PDF: { position: "-192px -64px", tooltip: "PDF Document" },
    /** The document search icon definition. */
    DOCUMENT_SEARCH: { position: "-192px -80px", tooltip: "Search Document" },
    /** The code document icon definition. */
    DOCUMENT_CODE: { position: "-192px -96px", tooltip: "Code Document" },
    /** The text document icon definition. */
    DOCUMENT_TEXT: { position: "-192px -112px", tooltip: "Text Document" },
    /** The contact icon definition. */
    CONTACT: { position: "-208px 0px", tooltip: "Contact" },
    /** The phone icon definition. */
    PHONE: { position: "-208px -16px", tooltip: "Phone Number" },
    /** The mobile phone icon definition. */
    PHONE_MOBILE: { position: "-208px -32px", tooltip: "Mobile Phone Number" },
    /** The comment icon definition. */
    COMMENT: { position: "-208px -48px", tooltip: "Comment" },
    /** The comments icon definition. */
    COMMENTS: { position: "-208px -64px", tooltip: "Comments" },
    /** The note icon definition. */
    NOTE: { position: "-208px -80px", tooltip: "Note" },
    /** The application icon definition. */
    APPLICATION: { position: "-224px 0px", tooltip: "Application" },
    /** The application terminal icon definition. */
    APPLICATION_TERMINAL: { position: "-224px -16px", tooltip: "Terminal" },
    /** The dialog icon definition. */
    DIALOG: { position: "-224px -32px", tooltip: "Dialog" },
    /** The script icon definition. */
    SCRIPT: { position: "-224px -48px", tooltip: "Script" },
    /** The component icon definition. */
    COMPONENT: { position: "-224px -64px", tooltip: "Component" },
    /** The components icon definition. */
    COMPONENTS: { position: "-224px -80px", tooltip: "Components" },
    /** The package icon definition. */
    PACKAGE: { position: "-224px -96px", tooltip: "Package" },
    /** The textfield icon definition. */
    TEXTFIELD: { position: "-224px -112px", tooltip: "Text Field" },
    /** The network drive icon definition. */
    DRIVE_NETWORK: { position: "-240px 0px", tooltip: "Network Drive" },
    /** The monitor icon definition. */
    MONITOR: { position: "-240px -16px", tooltip: "Monitor" },
    /** The keyboard icon definition. */
    KEYBOARD: { position: "-240px -32px", tooltip: "Keyboard" },
    /** The printer icon definition. */
    PRINTER: { position: "-240px -48px", tooltip: "Printer" },
    /** The server icon definition. */
    SERVER: { position: "-240px -64px", tooltip: "Server" },
    /** The disk icon definition. */
    DISK: { position: "-240px -80px", tooltip: "Disk" },
    /** The database icon definition. */
    DATABASE: { position: "-240px -96px", tooltip: "Database" },
    /** The email icon definition. */
    EMAIL: { position: "-240px -112px", tooltip: "Email" },
    /** The image icon definition. */
    IMAGE: { position: "-256px 0px", tooltip: "Image" },
    /** The calculator icon definition. */
    CALCULATOR: { position: "-256px -16px", tooltip: "Calculator" },
    /** The home icon definition. */
    HOME: { position: "-256px -32px", tooltip: "Home" },
    /** The book icon definition. */
    BOOK: { position: "-256px -48px", tooltip: "Book" },
    /** The open book icon definition. */
    BOOK_OPEN: { position: "-256px -64px", tooltip: "Book" },
    /** The clock icon definition. */
    CLOCK: { position: "-256px -80px", tooltip: "Clock" },
    /** The delay icon definition. */
    DELAY: { position: "-256px -96px", tooltip: "Delay" }
});
