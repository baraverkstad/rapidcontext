/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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
 * Creates a new icon widget.
 *
 * @constructor
 * @param {string|Object|Node} def the icon ref, class, attributes or DOM node
 * @param {string} [def.ref] the predefined icon name (reference)
 * @param {string} [def.class] the icon CSS class names
 * @param {string} [def.url] the icon image URL
 * @param {string} [def.position] the icon image position
 * @param {number} [def.width] the icon width (in pixels)
 * @param {number} [def.height] the icon height (in pixels)
 * @param {string} [def.tooltip] the icon tooltip text
 * @param {boolean} [def.disabled] the disabled widget flag, defaults to
 *            false
 * @param {boolean} [def.hidden] the hidden widget flag, defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The icon widget class. Used to provide a small clickable image, using
 *     the `<i>` HTML element. The icons can be either image- or font-based,
 *     causing slightly different behaviours with respect to size. A number of
 *     predefined icons suitable for different actions are available (using the
 *     Font-Awesome icon font).
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * var style = { "margin-left": "3px" };
 * var dataReload = RapidContext.Widget.Icon({ ref: "RELOAD", style: style });
 * var dataLoading = RapidContext.Widget.Icon({ ref: "LOADING", hidden: true, style: style });
 * var h3 = RapidContext.UI.H3({}, "Data List:", dataReload, dataLoading);
 *
 * @example <caption>User Interface XML</caption>
 * <h3>
 *   Data List:
 *   <Icon id="dataReload" ref="RELOAD" style="margin-left: 3px;" />
 *   <Icon id="dataLoading" ref="LOADING" hidden="true" style="margin-left: 3px;" />
 * </h3>
 */
RapidContext.Widget.Icon = function (def) {
    const o = (def && def.nodeType === 1) ? def : document.createElement("i");
    if (!RapidContext.Widget.isWidget(o, "Icon")) {
        RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Icon);
        o.addClass("widgetIcon");
    }
    o.setAttrs((def && def.nodeType === 1) ? {} : def);
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
 * Updates the icon or HTML DOM node attributes.
 *
 * @param {string|Object} attrs the icon ref, class or attributes to set
 * @param {string} [attrs.ref] the predefined icon name (reference)
 * @param {string} [attrs.class] the icon CSS class names
 * @param {string} [attrs.url] the icon image URL
 * @param {string} [attrs.position] the icon image position
 * @param {number} [attrs.width] the icon width (in pixels)
 * @param {number} [attrs.height] the icon height (in pixels)
 * @param {string} [attrs.tooltip] the icon tooltip text
 * @param {boolean} [attrs.disabled] the disabled widget flag
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Icon.prototype.setAttrs = function (attrs) {
    if (typeof(attrs) === "string") {
        const key = (attrs in RapidContext.Widget.Icon) ? "ref" : "class";
        attrs = { [key]: attrs };
    } else {
        attrs = { ...attrs };
    }
    while ("ref" in attrs) {
        const def = RapidContext.Widget.Icon[attrs.ref] || {};
        delete attrs.ref;
        attrs = { ...def, ...attrs };
    }
    const styles = {};
    if ("url" in attrs) {
        this.addClass("widgetIconSprite");
        styles.backgroundImage = `url('${attrs.url}')`;
        delete attrs.url;
    }
    if ("position" in attrs) {
        styles.backgroundPosition = attrs.position;
        delete attrs.position;
    }
    if ("width" in attrs) {
        styles.width = `${attrs.width}px`;
        delete attrs.width;
    }
    if ("height" in attrs) {
        styles.height = `${attrs.height}px`;
        delete attrs.height;
    }
    if ("tooltip" in attrs) {
        attrs.title = attrs.title || attrs.tooltip;
        delete attrs.tooltip;
    }
    Object.keys(styles).length && this.setStyle(styles);
    this.__setAttrs(attrs);
};

Object.assign(RapidContext.Widget.Icon, {
    /**
     * The default icon definition, inherited by all others.
     * @memberof RapidContext.Widget.Icon
     */
    DEFAULT: { url: "rapidcontext/files/images/icons/icons_16x16.png", width: 16, height: 16 },
    /**
     * The blank icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    BLANK: { ref: "DEFAULT", position: "0px 0px", style: { cursor: "default" } },
    /**
     * The loading icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    LOADING: { "class": "fa fa-refresh fa-spin", tooltip: "Loading..." },
    /**
     * The close icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CLOSE: { "class": "fa fa-times", tooltip: "Close" },
    /**
     * The close (inverse video) icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CLOSE_INVERSE: { ref: "DEFAULT", position: "0px -32px", tooltip: "Close" },
    /**
     * The close active icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CLOSE_ACTIVE: { ref: "DEFAULT", position: "0px -48px", tooltip: "Close" },
    /**
     * The bluewhite close icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CLOSE_BLUEWHITE: { ref: "DEFAULT", position: "0px -64px", tooltip: "Close" },
    /**
     * The blue close icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CLOSE_BLUE: { ref: "DEFAULT", position: "0px -80px", tooltip: "Close" },
    /**
     * The resize icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    RESIZE: { ref: "DEFAULT", position: "0px -96px", style: { cursor: "se-resize" } },
    /**
     * The ok icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    OK: { "class": "fa fa-check", tooltip: "OK" },
    /**
     * The stop icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    STOP: { "class": "fa fa-stop", tooltip: "Stop" },
    /**
     * The yes icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    YES: { "class": "fa fa-check-square", tooltip: "Yes" },
    /**
     * The no icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    NO: { "class": "fa fa-square-o", tooltip: "No" },
    /**
     * The cancel icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CANCEL: { "class": "fa fa-times", tooltip: "Cancel" },
    /**
     * The up icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    UP: { "class": "fa fa-chevron-up", tooltip: "Move up" },
    /**
     * The down icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOWN: { "class": "fa fa-chevron-down", tooltip: "Move down" },
    /**
     * The left icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    LEFT: { "class": "fa fa-chevron-left", tooltip: "Move left" },
    /**
     * The right icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    RIGHT: { "class": "fa fa-chevron-right", tooltip: "Move right" },
    /**
     * The first icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FIRST: { "class": "fa fa-step-backward", tooltip: "First" },
    /**
     * The last icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    LAST: { "class": "fa fa-step-forward", tooltip: "Last" },
    /**
     * The previous icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PREVIOUS: { "class": "fa fa-caret-left", tooltip: "Previous" },
    /**
     * The next icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    NEXT: { "class": "fa fa-caret-right", tooltip: "Next" },
    /**
     * The plus icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PLUS: { "class": "fa fa-plus-square-o", tooltip: "Show" },
    /**
     * The minus icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    MINUS: { "class": "fa fa-minus-square-o", tooltip: "Hide" },
    /**
     * The remove icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    REMOVE: { "class": "fa fa-minus-square color-danger", tooltip: "Remove" },
    /**
     * The add icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    ADD: { "class": "fa fa-plus-square color-success", tooltip: "Add" },
    /**
     * The copy icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    COPY: { ref: "DEFAULT", position: "-48px -32px", tooltip: "Copy" },
    /**
     * The cut icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CUT: { ref: "DEFAULT", position: "-48px -48px", tooltip: "Cut" },
    /**
     * The delete icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DELETE: { ref: "DEFAULT", position: "-48px -64px", tooltip: "Delete" },
    /**
     * The reload icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    RELOAD: { "class": "fa fa-refresh", tooltip: "Reload" },
    /**
     * The edit icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    EDIT: { "class": "fa fa-pencil-square color-warning", tooltip: "Edit" },
    /**
     * The white edit icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    EDIT_WHITE: { ref: "DEFAULT", position: "-48px -112px", tooltip: "Edit" },
    /**
     * The layout edit icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    EDIT_LAYOUT: { ref: "DEFAULT", position: "-48px -128px", tooltip: "Edit" },
    /**
     * The search icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    SEARCH: { "class": "fa fa-search", tooltip: "Search" },
    /**
     * The expand icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    EXPAND: { "class": "fa fa-external-link", tooltip: "Open in new window" },
    /**
     * The asterisk icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    ASTERISK: { "class": "fa fa-asterisk", tooltip: "Mark" },
    /**
     * The select icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    SELECT: { "class": "fa fa-star", tooltip: "Select / Unselect" },
    /**
     * The like icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    LIKE: { "class": "fa fa-heart", tooltip: "Like / Unlike" },
    /**
     * The flag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FLAG: { "class": "fa fa-flag", tooltip: "Flag" },
    /**
     * The red flag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FLAG_RED: { ref: "DEFAULT", position: "-64px -48px", tooltip: "Flag" },
    /**
     * The blue flag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FLAG_BLUE: { ref: "DEFAULT", position: "-64px -64px", tooltip: "Flag" },
    /**
     * The green flag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FLAG_GREEN: { ref: "DEFAULT", position: "-64px -80px", tooltip: "Flag" },
    /**
     * The yellow flag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FLAG_YELLOW: { ref: "DEFAULT", position: "-64px -96px", tooltip: "Flag" },
    /**
     * The tag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    TAG: { "class": "fa fa-tag", tooltip: "Tag" },
    /**
     * The red tag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    TAG_RED: { ref: "DEFAULT", position: "-64px -112px", tooltip: "Tag" },
    /**
     * The blue tag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    TAG_BLUE: { ref: "DEFAULT", position: "-64px -128px", tooltip: "Tag" },
    /**
     * The green tag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    TAG_GREEN: { ref: "DEFAULT", position: "-64px -144px", tooltip: "Tag" },
    /**
     * The yellow tag icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    TAG_YELLOW: { ref: "DEFAULT", position: "-64px -160px", tooltip: "Tag" },
    /**
     * The options icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    OPTIONS: { "class": "fa fa-cog", tooltip: "Options" },
    /**
     * The configure icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CONFIG: { "class": "fa fa-wrench", tooltip: "Configure" },
    /**
     * The attach file icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    ATTACH: { "class": "fa fa-paperclip", tooltip: "Attach file" },
    /**
     * The automatic icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    AUTOMATIC: { "class": "fa fa-magic", tooltip: "Automatic actions" },
    /**
     * The export icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    EXPORT: { ref: "DEFAULT", position: "-80px -64px", tooltip: "Export" },
    /**
     * The information icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    INFO: { "class": "fa fa-info-circle", tooltip: "Information" },
    /**
     * The help icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    HELP: { "class": "fa fa-life-ring", tooltip: "Help" },
    /**
     * The warning icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    WARNING: { "class": "fa fa-exclamation-triangle", tooltip: "Warning" },
    /**
     * The error icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    ERROR: { "class": "fa fa-exclamation-circle", tooltip: "Error" },
    /**
     * The bar chart icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    BARCHART: { ref: "DEFAULT", position: "-112px 0px", tooltip: "Bar chart" },
    /**
     * The line chart icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    LINECHART: { ref: "DEFAULT", position: "-112px -16px", tooltip: "Line chart" },
    /**
     * The curve chart icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CURVECHART: { ref: "DEFAULT", position: "-112px -32px", tooltip: "Curve chart" },
    /**
     * The pie chart icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PIECHART: { ref: "DEFAULT", position: "-112px -48px", tooltip: "Pie chart" },
    /**
     * The organization chart icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    ORGCHART: { ref: "DEFAULT", position: "-112px -64px", tooltip: "Organization chart" },
    /**
     * The web feed icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FEED: { ref: "DEFAULT", position: "-128px 0px", tooltip: "RSS/Atom feed" },
    /**
     * The RSS icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    RSS: { ref: "DEFAULT", position: "-128px -16px", tooltip: "RSS feed" },
    /**
     * The CSS icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CSS: { ref: "DEFAULT", position: "-128px -32px", tooltip: "CSS" },
    /**
     * The HTML icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    HTML: { ref: "DEFAULT", position: "-128px -48px", tooltip: "HTML" },
    /**
     * The XHTML icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    XHTML: { ref: "DEFAULT", position: "-128px -64px", tooltip: "XHTML" },
    /**
     * The font icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FONT: { ref: "DEFAULT", position: "-144px 0px", tooltip: "Font" },
    /**
     * The style icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    STYLE: { ref: "DEFAULT", position: "-144px -16px", tooltip: "Style" },
    /**
     * The text format icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    TEXT_FORMAT: { ref: "DEFAULT", position: "-144px -32px", tooltip: "Text format" },
    /**
     * The calendar icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CALENDAR: { ref: "DEFAULT", position: "-144px -48px", tooltip: "Calendar" },
    /**
     * The date icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DATE: { ref: "DEFAULT", position: "-144px -64px", tooltip: "Date" },
    /**
     * The layout icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    LAYOUT: { ref: "DEFAULT", position: "-144px -80px", tooltip: "Layout" },
    /**
     * The table icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    TABLE: { ref: "DEFAULT", position: "-144px -96px", tooltip: "Table" },
    /**
     * The sum icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    SUM: { ref: "DEFAULT", position: "-144px -112px", tooltip: "Sum" },
    /**
     * The vector icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    VECTOR: { ref: "DEFAULT", position: "-144px -128px", tooltip: "Vectors" },
    /**
     * The color icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    COLOR: { ref: "DEFAULT", position: "-144px -144px", tooltip: "Color" },
    /**
     * The HTML source icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    HTML_SOURCE: { ref: "DEFAULT", position: "-144px -160px", tooltip: "HTML source" },
    /**
     * The plug-in icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PLUGIN: { ref: "DEFAULT", position: "-160px 0px", tooltip: "Plug-in" },
    /**
     * The add plug-in icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PLUGIN_ADD: { ref: "DEFAULT", position: "-160px -16px", tooltip: "Add plug-in" },
    /**
     * The remove plug-in icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PLUGIN_REMOVE: { ref: "DEFAULT", position: "-160px -32px", tooltip: "Remove plug-in" },
    /**
     * The inactive plug-in icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PLUGIN_INACTIVE: { ref: "DEFAULT", position: "-160px -48px", tooltip: "Inactive plug-in" },
    /**
     * The plug-in error icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PLUGIN_ERROR: { ref: "DEFAULT", position: "-160px -64px", tooltip: "Plug-in error" },
    /**
     * The user icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    USER: { "class": "fa fa-user", tooltip: "User" },
    /**
     * The group icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    GROUP: { "class": "fa fa-users", tooltip: "Group" },
    /**
     * The folder icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FOLDER: { ref: "DEFAULT", position: "-176px 0px", tooltip: "Folder" },
    /**
     * The add folder icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    FOLDER_ADD: { ref: "DEFAULT", position: "-176px -16px", tooltip: "Add folder" },
    /**
     * The lock icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    LOCK: { "class": "fa fa-lock", tooltip: "Lock" },
    /**
     * The key icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    KEY: { "class": "fa fa-key", tooltip: "Key" },
    /**
     * The document icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOCUMENT: { ref: "DEFAULT", position: "-192px 0px", tooltip: "Document" },
    /**
     * The Word document icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOCUMENT_WORD: { ref: "DEFAULT", position: "-192px -16px", tooltip: "Word document" },
    /**
     * The Excel document icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOCUMENT_EXCEL: { ref: "DEFAULT", position: "-192px -32px", tooltip: "Excel document" },
    /**
     * The Office document icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOCUMENT_OFFICE: { ref: "DEFAULT", position: "-192px -48px", tooltip: "Office document" },
    /**
     * The PDF document icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOCUMENT_PDF: { ref: "DEFAULT", position: "-192px -64px", tooltip: "PDF document" },
    /**
     * The document search icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOCUMENT_SEARCH: { ref: "DEFAULT", position: "-192px -80px", tooltip: "Search document" },
    /**
     * The code document icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOCUMENT_CODE: { ref: "DEFAULT", position: "-192px -96px", tooltip: "Code document" },
    /**
     * The text document icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DOCUMENT_TEXT: { ref: "DEFAULT", position: "-192px -112px", tooltip: "Text document" },
    /**
     * The contact icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CONTACT: { ref: "DEFAULT", position: "-208px 0px", tooltip: "Contact" },
    /**
     * The phone icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PHONE: { ref: "DEFAULT", position: "-208px -16px", tooltip: "Phone number" },
    /**
     * The mobile phone icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PHONE_MOBILE: { ref: "DEFAULT", position: "-208px -32px", tooltip: "Mobile phone number" },
    /**
     * The comment icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    COMMENT: { ref: "DEFAULT", position: "-208px -48px", tooltip: "Comment" },
    /**
     * The comments icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    COMMENTS: { ref: "DEFAULT", position: "-208px -64px", tooltip: "Comments" },
    /**
     * The note icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    NOTE: { ref: "DEFAULT", position: "-208px -80px", tooltip: "Note" },
    /**
     * The application icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    APPLICATION: { ref: "DEFAULT", position: "-224px 0px", tooltip: "Application" },
    /**
     * The application terminal icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    APPLICATION_TERMINAL: { ref: "DEFAULT", position: "-224px -16px", tooltip: "Terminal" },
    /**
     * The dialog icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DIALOG: { ref: "DEFAULT", position: "-224px -32px", tooltip: "Dialog" },
    /**
     * The script icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    SCRIPT: { ref: "DEFAULT", position: "-224px -48px", tooltip: "Script" },
    /**
     * The component icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    COMPONENT: { "class": "fa fa-cube", tooltip: "Component" },
    /**
     * The components icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    COMPONENTS: { "class": "fa fa-cubes", tooltip: "Components" },
    /**
     * The package icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PACKAGE: { ref: "DEFAULT", position: "-224px -96px", tooltip: "Package" },
    /**
     * The textfield icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    TEXTFIELD: { ref: "DEFAULT", position: "-224px -112px", tooltip: "Text field" },
    /**
     * The network drive icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DRIVE_NETWORK: { ref: "DEFAULT", position: "-240px 0px", tooltip: "Network drive" },
    /**
     * The monitor icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    MONITOR: { ref: "DEFAULT", position: "-240px -16px", tooltip: "Monitor" },
    /**
     * The keyboard icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    KEYBOARD: { ref: "DEFAULT", position: "-240px -32px", tooltip: "Keyboard" },
    /**
     * The printer icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    PRINTER: { ref: "DEFAULT", position: "-240px -48px", tooltip: "Printer" },
    /**
     * The server icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    SERVER: { ref: "DEFAULT", position: "-240px -64px", tooltip: "Server" },
    /**
     * The disk icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DISK: { ref: "DEFAULT", position: "-240px -80px", tooltip: "Disk" },
    /**
     * The database icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DATABASE: { ref: "DEFAULT", position: "-240px -96px", tooltip: "Database" },
    /**
     * The email icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    EMAIL: { ref: "DEFAULT", position: "-240px -112px", tooltip: "Email" },
    /**
     * The image icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    IMAGE: { ref: "DEFAULT", position: "-256px 0px", tooltip: "Image" },
    /**
     * The calculator icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CALCULATOR: { ref: "DEFAULT", position: "-256px -16px", tooltip: "Calculator" },
    /**
     * The home icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    HOME: { ref: "DEFAULT", position: "-256px -32px", tooltip: "Home" },
    /**
     * The book icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    BOOK: { ref: "DEFAULT", position: "-256px -48px", tooltip: "Book" },
    /**
     * The open book icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    BOOK_OPEN: { ref: "DEFAULT", position: "-256px -64px", tooltip: "Book" },
    /**
     * The clock icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    CLOCK: { ref: "DEFAULT", position: "-256px -80px", tooltip: "Clock" },
    /**
     * The delay icon definition.
     * @memberof RapidContext.Widget.Icon
     */
    DELAY: { ref: "DEFAULT", position: "-256px -96px", tooltip: "Delay" }
});
