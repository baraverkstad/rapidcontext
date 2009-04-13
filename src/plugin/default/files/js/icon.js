/**
 * Creates a new icon image definition. Icon images are not normal
 * widget, since they do not contain an actual HTML DOM node.
 * Instead a method for creating an HTML DOM node from the definition
 * is provided.
 *
 * @constructor
 * @param src                the image source URL
 * @param width              the image width, or null for none
 * @param height             the image height, or null for none
 * @param tooltip            the tooltip string, or null for none
 * @param preload            the preload flag, defaults to false
 */
function Icon(src, width, height, tooltip, preload) {
    this.src = src;
    this.width = width;
    this.height = height;
    this.tooltip = tooltip;
    if (preload) {
        this.createElement();
    }
}

/**
 * Registers an icon click handler.
 *
 * @memberOf Icon
 * @param elem               the HTML DOM node element
 * @param method             the callback method
 * @param obj                the callback object, or null
 */
Icon.registerOnClick = function(elem, method, obj) {
    if (method != null) {
        elem.style.cursor = "pointer";
        elem.onclick = FunctionUtil.bind(method, obj, elem);
    } else {
        elem.style.cursor = "default";
        elem.onclick = function() {
            // Default to no action
        }
    }
}

/**
 * Creates an HTML DOM node element for the icon.
 *
 * @return an HTML DOM node element
 */
Icon.prototype.createElement = function() {
    var elem = document.createElement("img");
    elem.src = this.src;
    if (this.width != null) {
        elem.width = this.width;
    }
    if (this.height != null) {
        elem.height = this.height;
    }
    if (this.tooltip != null) {
        elem.alt = this.tooltip;
        elem.title = this.tooltip;
    }
    return elem;
}

/**
 * The blank icon image.
 *
 * @memberOf Icon
 */
Icon.BLANK = new Icon("images/icons/blank.gif", 16, 16, null, true);

/**
 * The close icon image.
 *
 * @memberOf Icon
 */
// TODO: add automatic support for mouseover image change
Icon.CLOSE = new Icon("images/icons/close.gif", 16, 16, "Close", true);

/**
 * The resize icon image.
 *
 * @memberOf Icon
 */
Icon.RESIZE = new Icon("images/icons/resize-handle.gif", 16, 16, null, false);

/**
 * The plus icon image.
 *
 * @memberOf Icon
 */
Icon.PLUS = new Icon("images/icons/plus.gif", 16, 16, "Show", true);

/**
 * The minus icon image.
 *
 * @memberOf Icon
 */
Icon.MINUS = new Icon("images/icons/minus.gif", 16, 16, "Hide", true);

/**
 * The config icon image.
 *
 * @memberOf Icon
 */
Icon.CONFIG = new Icon("images/icons/config.gif", 16, 16, "Configure", false);

/**
 * The delay icon image.
 *
 * @memberOf Icon
 */
Icon.DELAY = new Icon("images/icons/delay.gif", 16, 16, "Configure Delay", false);

/**
 * The reload icon image.
 *
 * @memberOf Icon
 */
Icon.RELOAD = new Icon("images/icons/reload.gif", 16, 16, "Reload", true);

/**
 * The loading icon image.
 *
 * @memberOf Icon
 */
Icon.LOADING = new Icon("images/icons/loading.gif", 16, 16, "Loading...", true);

/**
 * The search icon image.
 *
 * @memberOf Icon
 */
Icon.SEARCH = new Icon("images/icons/magnifier.gif", 16, 16, "Search", false);

/**
 * The add icon image.
 *
 * @memberOf Icon
 */
Icon.ADD = new Icon("images/icons/add.gif", 16, 16, "Add", false);

/**
 * The remove icon image.
 *
 * @memberOf Icon
 */
Icon.REMOVE = new Icon("images/icons/remove.gif", 16, 16, "Remove", false);

/**
 * The edit icon image.
 *
 * @memberOf Icon
 */
Icon.EDIT = new Icon("images/icons/edit.gif", 16, 16, "Edit", false);

/**
 * The delete icon image.
 *
 * @memberOf Icon
 */
Icon.DELETE = new Icon("images/icons/trash.gif", 16, 16, "Clear / Delete", false);

/**
 * The select icon image.
 *
 * @memberOf Icon
 */
Icon.SELECT = new Icon("images/icons/select.gif", 16, 16, "Select / Unselect", false);

/**
 * The cut icon image.
 *
 * @memberOf Icon
 */
Icon.CUT = new Icon("images/icons/cut.gif", 16, 16, "Cut", false);

/**
 * The export icon image.
 *
 * @memberOf Icon
 */
Icon.EXPORT = new Icon("images/icons/export.gif", 16, 16, "Export", false);

/**
 * The expand icon image.
 *
 * @memberOf Icon
 */
Icon.EXPAND = new Icon("images/icons/expand.gif", 16, 16, "Expand", false);

/**
 * The comment icon image.
 *
 * @memberOf Icon
 */
Icon.COMMENT = new Icon("images/icons/comment.gif", 16, 16, "Comment", false);

/**
 * The calendar icon image.
 *
 * @memberOf Icon
 */
Icon.CALENDAR = new Icon("images/icons/calendar.gif", 16, 16, "Calendar", false);

/**
 * The automatic icon image.
 *
 * @memberOf Icon
 */
Icon.AUTOMATIC = new Icon("images/icons/automatic.gif", 16, 16, "Automatic Processing", false);

/**
 * The plugin icon image.
 *
 * @memberOf Icon
 */
Icon.PLUGIN = new Icon("images/icons/plugin.gif", 16, 16, "Plug-in", false);

/**
 * The folder icon image.
 *
 * @memberOf Icon
 */
Icon.FOLDER = new Icon("images/icons/folder.gif", 16, 16, null, false);

/**
 * The document icon image.
 *
 * @memberOf Icon
 */
Icon.DOCUMENT = new Icon("images/icons/document.gif", 16, 16, null, false);
