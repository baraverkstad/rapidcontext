/**
 * Creates a new app instance.
 */
function HelpApp() {
    this._topics = null;
    this._currentUrl = "";
    this._historyHead = [];
    this._historyTail = [];
}

/**
 * Starts the app and initializes the UI.
 */
HelpApp.prototype.start = function() {
    MochiKit.Signal.connect(this.ui.topicReload, "onclick", this, "loadTopics");
    MochiKit.Signal.connect(this.ui.topicTree, "onexpand", this, "_treeOnExpand");
    MochiKit.Signal.connect(this.ui.topicTree, "onselect", this, "_treeOnSelect");
    MochiKit.Signal.connect(this.ui.contentPrev, "onclick", this, "_historyBack");
    MochiKit.Signal.connect(this.ui.contentNext, "onclick", this, "_historyForward");
    MochiKit.Signal.connect(this.ui.contentText, "onclick", this, "_handleClick");
    this.loadTopics();
};

/**
 * Stops the app.
 */
HelpApp.prototype.stop = function() {
    // Nothing to do here
};

/**
 * Loads all available topics and displays them in the topic tree
 * view. Currently this method does not fetch new topic data from
 * the server, but only unifies the topic data from apps and the
 * previously loaded platform topics.
 */
HelpApp.prototype.loadTopics = function() {
    this._topics = { child: {}, children: [], url: {} };
    this.ui.topicTree.removeAll();
    var apps = RapidContext.App.apps();
    for (var i = 0; i < apps.length; i++) {
        var app = apps[i];
        for (var j = 0; j < app.resources.length; j++) {
            var res = app.resources[j];
            if (res.topic != null) {
                this._addTopic(app.name + " (App)", MochiKit.Base.clone(res));
            }
        }
    }
    var source = "RapidContext Platform Documentation";
    var func = MochiKit.Base.method(this, "_addTopic", source);
    MochiKit.Base.map(func, this.resource.topicsBase);
    MochiKit.Base.map(func, this.resource.topicsJsApi);
    MochiKit.Base.map(func, this.resource.topicsMochiKit);
    MochiKit.Base.map(func, this.resource.topicsJava);
    MochiKit.Base.map(func, this.resource.topicsExternal);
    this._treeInsertChildren(this.ui.topicTree, this._topics);
    this.ui.topicTree.expandAll(1);
    if (this._currentUrl) {
        this.loadContent(this._currentUrl);
    }
};

/**
 * Adds a topic data object to the internal topic tree structure.
 *
 * @param {String} source the topic source
 * @param {Object} topic the topic data object
 */
HelpApp.prototype._addTopic = function (source, topic) {
    topic.source = topic.source || source;
    var path = topic.topic.split("/");
    var parent = this._topics;
    while (path.length > 1) {
        var name = path.shift();
        var temp = parent.child[name];
        if (!temp) {
            temp = { name: name, child: {}, children: [] };
            parent.child[name] = temp;
            parent.children.push(temp);
        }
        parent = temp;
    }
    var name = path.shift();
    if (parent.child[name]) {
        LOG.warning("Duplicated Help topic", topic.topic);
        topic = MochiKit.Base.update(parent.child[name], topic);
    } else {
        MochiKit.Base.update(topic, { name: name, child: {}, children: [] });
        parent.child[name] = topic;
        parent.children.push(topic);
    }
    if (topic.url) {
        this._topics.url[topic.url] = topic;
    }
};

/**
 * Adds the child topics to a node.
 *
 * @param {Tree/TreeNode} parentNode the parent tree or tree node
 * @param {Object} topic the parent topic
 */
HelpApp.prototype._treeInsertChildren = function (parentNode, topic) {
    for (var i = 0; i < topic.children.length; i++) {
        var child = topic.children[i];
        var icon = { ref: "BOOK_OPEN", tooltip: "Documentation Topic" };
        if (!child.url) {
            icon = "FOLDER";
        } else if (/https?:/.test(child.url)) {
            icon = { ref: "BOOK", tooltip: "External Documentation" };
        } else if (/#/.test(child.url)) {
            icon = { ref: "TAG_BLUE", tooltip: "Bookmark" };
        }
        var attrs = {
            name: child.name,
            folder: (child.children.length > 0),
            icon: icon
        };
        var node = RapidContext.Widget.TreeNode(attrs);
        node.data = child;
        parentNode.addAll(node);
    }
};

/**
 * Expands the tree topic corresponding to a specified URL. The URL
 * must exactly match a topic URL, or no match will be found.
 *
 * @param {String} url the URL to search for
 *
 * @return {TreeNode} the TreeNode widget for the matching topic
 */
HelpApp.prototype._treeExpandUrl = function (url) {
    if (this._treeBlockEvents) {
        return this.ui.topicTree.selectedChild();
    } else {
        var topic = this._topics.url[url] || this._topics.url[url.replace(/#.*/, "")];
        if (topic) {
            var path = topic.topic.split("/");
            for (var i = 0; i < path.length; i++) {
                this.ui.topicTree.findByPath(path.slice(0, i + 1)).expand();
            }
            var node = this.ui.topicTree.findByPath(path);
            this._treeBlockEvents = true;
            node.select();
            this._treeBlockEvents = false;
            return node;
        }
    }
    return null;
};

/**
 * Handles the tree expand and collapse events.
 *
 * @param {TreeNode} node the tree node expanded or collapsed
 */
HelpApp.prototype._treeOnExpand = function (node) {
    var topic = node.data;
    if (node.isExpanded && topic.children.length != node.getChildNodes().length) {
        this._treeInsertChildren(node, topic);
    }
};

/**
 * Handles the tree select events.
 */
HelpApp.prototype._treeOnSelect = function () {
    if (!this._treeBlockEvents) {
        var node = this.ui.topicTree.selectedChild();
        if (node && node.data && node.data.url) {
            this._treeBlockEvents = true;
            this.loadContent(node.data.url);
            this._treeBlockEvents = false;
        }
    }
};

/**
 * Moves one step back in history (if possible).
 */
HelpApp.prototype._historyBack = function () {
    if (!this.ui.contentPrev.isDisabled()) {
        this._historyBlockUpdates = true;
        this._historyHead.push(this._currentUrl);
        this.loadContent(this._historyTail.pop());
        this._historyBlockUpdates = false;
    }
};

/**
 * Moves one step forward in history (if possible).
 */
HelpApp.prototype._historyForward = function () {
    if (!this.ui.contentNext.isDisabled()) {
        this._historyBlockUpdates = true;
        this._historyTail.push(this._currentUrl);
        this.loadContent(this._historyHead.pop());
        this._historyBlockUpdates = false;
    }
};

/**
 * Saves the current URL to the history. Also clears the forward
 * history if not already empty.
 */
HelpApp.prototype._historySave = function (url) {
    if (!this._historyBlockUpdates) {
        this._historyHead = [];
        if (this._currentUrl) {
            this._historyTail.push(this._currentUrl);
        }
    }
};

/**
 * Clears the content view from any loaded topic data.
 */
HelpApp.prototype.clearContent = function() {
    this._currentUrl = "";
    this.ui.contentLoading.hide();
    this.ui.contentPrev.setAttrs({ disabled: (this._historyTail.length <= 0) });
    this.ui.contentNext.setAttrs({ disabled: (this._historyHead.length <= 0) });
    this.ui.contentLink.className = "hidden";
    MochiKit.DOM.replaceChildNodes(this.ui.contentInfo);
    MochiKit.DOM.replaceChildNodes(this.ui.contentText);
};

/**
 * Loads a new HTML document into the content view. The document is
 * loaded from a specified URL and the tree is updated to select the
 * topic matching the URL (if any). Note that for external URL:s, a
 * separate window (tab) is opened instead.
 *
 * @param {String} url the content URL (HTML document)
 */
HelpApp.prototype.loadContent = function (url) {
    var node = this._treeExpandUrl(url);
    var fileUrl = url.replace(/#.*/, "");
    if (/https?:/.test(url)) {
        window.open(url);
    } else if (/#.+/.test(url) && this._currentUrl.indexOf(fileUrl) == 0) {
        this._currentUrl = url;
        this._scrollLink(url.replace(/.*#/, ""));
    } else {
        this._historySave();
        this.clearContent();
        this._currentUrl = url;
        this.ui.contentLoading.show();
        var source = (node && node.data) ? node.data.source || "" : "";
        MochiKit.DOM.replaceChildNodes(this.ui.contentInfo, source);
        var d = RapidContext.App.loadText(fileUrl, null, { timeout: 60 });
        d.addBoth(MochiKit.Base.method(this, "_callbackContent"));
    }
};

/**
 * Callback function for content HTML document retrieval.
 */
HelpApp.prototype._callbackContent = function(data) {
    this.ui.contentLoading.hide();
    if (data instanceof Error) {
        RapidContext.UI.showError(data);
    } else if (typeof(data) == "string") {
        MochiKit.DOM.setNodeAttribute(this.ui.contentLink, "href", this._currentUrl);
        this.ui.contentLink.className = "";
        this._showContentHtml(data);
        if (/#.+/.test(this._currentUrl)) {
            this._scrollLink(this._currentUrl.replace(/.*#/, ""));
        } else {
            RapidContext.Util.setScrollOffset(this.ui.contentScroll, 0);
        }
    } else {
        MochiKit.DOM.replaceChildNodes(this.ui.contentInfo, "Not Found");
    }
};

/**
 * Displays content HTML data. This method replaces all links in the
 * HTML document with script handlers to be able to open new
 * documents either in new windows or in the same content pane.
 *
 * @param {String} html the HTML data to display
 */
HelpApp.prototype._showContentHtml = function(html) {
    html = html.replace(/^[\s\S]*<body[^>]*>/i, "");
    html = html.replace(/<\/body>[\s\S]*$/i, "");
    html = html.replace(/^[\s\S]*<!--START-->/, "");
    html = html.replace(/<!--END-->[\s\S]*$/, "");
    html = html.replace(/^[\s\S]*(<div class="document">)/i, "$1");
    this.ui.contentText.innerHTML = html;
    var baseUrl = RapidContext.Util.resolveURI("");
    var nodes = this.ui.contentText.getElementsByTagName("a");
    for (var i = 0; i < nodes.length; i++) {
        var href = nodes[i].getAttribute("href");
        if (href && href != "") {
            href = RapidContext.Util.resolveURI(href, this._currentUrl);
            if (href.indexOf(baseUrl) == 0) {
                href = href.substring(baseUrl.length);
            }
            if (nodes[i].hasAttribute("target")) {
                nodes[i].setAttribute("target", "doc");
            }
            if (href.indexOf("://") > 0) {
                nodes[i].setAttribute("target", "doc");
            } else {
                nodes[i].setAttribute("href", href);
            }
        }
    }
    var nodes = this.ui.contentText.getElementsByTagName("img");
    for (var i = 0; i < nodes.length; i++) {
        var href = nodes[i].getAttribute("src");
        if (href && href != "") {
            href = RapidContext.Util.resolveURI(href, this._currentUrl);
            if (href.indexOf(baseUrl) == 0) {
                href = href.substring(baseUrl.length);
            }
            if (href.indexOf("://") < 0) {
                nodes[i].setAttribute("src", href);
            }
        }
    }
};

/**
 * Handles click events in the content text.
 */
HelpApp.prototype._handleClick = function (evt) {
    var elem = evt.target();
    if (elem.tagName != "A") {
        elem = MochiKit.DOM.getFirstParentByTagAndClassName(elem, "A");
    }
    if (elem && elem.hasAttribute("href") && !elem.hasAttribute("target")) {
        evt.stop();
        var href = elem.getAttribute("href");
        var baseUrl = RapidContext.Util.resolveURI("");
        if (href.indexOf(baseUrl) == 0) {
            href = href.substring(baseUrl.length);
        }
        this.loadContent(href);
    }
};

/**
 * Scrolls the content pane to make the specified link name visible.
 *
 * @param {String} name the link name attribute
 */
HelpApp.prototype._scrollLink = function (name) {
    var selector = "a[name='" + name + "'], *[id='" + name + "']";
    var ctx = $(this.ui.contentText).find(selector);
    if (ctx.length) {
        var elem = ctx[0];
        this.ui.contentScroll.scrollTop = elem.offsetTop;
        this.ui.contentLocator.animate({ effect: "cancel" });
        MochiKit.Style.setElementPosition(this.ui.contentLocator, { y: elem.offsetTop });
        MochiKit.Style.setOpacity(this.ui.contentLocator, 0);
        MochiKit.Style.showElement(this.ui.contentLocator);
        var opts = {
            effect: "Opacity",
            duration: 0.8,
            transition: function (pos) {
                if (pos < 0.2) {
                    pos = pos / 0.2;
                } else {
                    pos = 1 - (pos - 0.2) / 0.8;
                }
                return MochiKit.Visual.Transitions.sinoidal(pos);
            },
            afterFinish: function (effect) {
                MochiKit.Style.setOpacity(effect.element, 0);
            }
        }
        this.ui.contentLocator.animate(opts);
    }
};
