/**
 * Creates a new app instance.
 */
function HelpApp() {
    this._topics = null;
    this._topicUrls = null;
    this._currentUrl = "";
}

/**
 * Starts the app and initializes the UI.
 */
HelpApp.prototype.start = function () {
    MochiKit.Signal.connect(this.ui.topicReload, "onclick", this, "loadTopics");
    MochiKit.Signal.connect(this.ui.topicTree, "onexpand", this, "_treeOnExpand");
    MochiKit.Signal.connect(this.ui.topicTree, "onselect", this, "_treeOnSelect");
    MochiKit.Signal.connect(this.ui.contentText, "onclick", this, "_handleClick");
    this.loadTopics();
};

/**
 * Stops the app.
 */
HelpApp.prototype.stop = function () {
    // Nothing to do here
};

/**
 * Loads all available topics and displays them in the topic tree
 * view. Currently this method does not fetch new topic data from
 * the server, but only unifies the topic data from apps and the
 * previously loaded platform topics.
 */
HelpApp.prototype.loadTopics = function () {
    var root = this._topics = { path: [], child: {}, children: [] };
    var topicUrls = this._topicUrls = {};

    // Add a topic path to a parent (and its children)
    function addPath(parent, path) {
        while (path.length > 0) {
            var name = path.shift();
            var child = parent.child[name];
            if (!child) {
                child = { name: name, child: {}, children: [] };
                child.path = parent.path.slice();
                child.path.push(name);
                parent.child[name] = child;
                parent.children.push(child);
            }
            parent = child;
        }
        return parent;
    }

    // Add a single help topic (and children if any)
    function add(parent, source, obj) {
        var topic = addPath(parent, obj.topic.split("/"));
        if (topic.source) {
            RapidContext.Log.warn("Duplicated Help topic, possibly overwritten", obj.topic);
        }
        topic.source = obj.source || source;
        if (obj.url) {
            topic.url = obj.url;
            topicUrls[obj.url] = topic;
        }
        if (obj.external) {
            topic.url = RapidContext.Util.resolveURI(obj.url);
        }
        addAll(topic, topic.source, obj.children);
    }

    // Add a list of help topics
    function addAll(parent, source, obj) {
        if (MochiKit.Base.isArrayLike(obj)) {
            for (var i = 0; i < obj.length; i++) {
                addAll(parent, source, obj[i]);
            }
        } else if (obj && typeof(obj.topic) == "string") {
            add(parent, source, obj);
        }
    }

    // Add app topics
    var apps = RapidContext.App.apps();
    for (var i = 0; i < apps.length; i++) {
        addAll(root, apps[i].name + " (App)", apps[i].resources);
    }

    // Add platform topics
    var source = "RapidContext Platform Documentation";
    addAll(root, source, this.resource.topicsBase);
    addAll(root, source, this.resource.topicsJsApi);
    addAll(root, source, this.resource.topicsJsEnv);
    addAll(root, source, this.resource.topicsJava);

    // Update topics tree
    this.ui.topicTree.removeAll();
    this._treeInsertChildren(this.ui.topicTree, root);
    this.ui.topicTree.expandAll(1);
    if (this._currentUrl) {
        this.loadContent(this._currentUrl);
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
        var attrs = {
            name: child.name,
            folder: (child.children.length > 0)
        };
        if (/^https?:/.test(child.url)) {
            attrs.icon = "fa fa-fw fa-external-link-square";
        } else if (/#/.test(child.url)) {
            attrs.icon = "fa fa-fw fa-bookmark-o";
        } else if (child.url) {
            attrs.icon = "fa fa-fw fa-bookmark";
        }
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
        var topic = this._topicUrls[url] || this._topicUrls[url.replace(/#.*/, "")];
        if (topic) {
            var path = topic.path;
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
 * @param {Event} evt the tree node expand event
 */
HelpApp.prototype._treeOnExpand = function (evt) {
    var node = evt.event().detail.node;
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
 * Clears the content view from any loaded topic data.
 */
HelpApp.prototype.clearContent = function () {
    this._currentUrl = "";
    this.ui.contentLoading.hide();
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
    if (/^https?:/.test(url)) {
        window.open(url);
    } else if (url.includes("#") && this._currentUrl.startsWith(fileUrl)) {
        this._currentUrl = url;
        this._scrollLink(url.replace(/.*#/, ""));
    } else {
        this.clearContent();
        this._currentUrl = url;
        this.ui.contentLoading.show();
        var source = (node && node.data) ? node.data.source || "" : "";
        MochiKit.DOM.replaceChildNodes(this.ui.contentInfo, source);
        RapidContext.App.loadText(fileUrl)
            .then(this._callbackContent.bind(this))
            .catch(RapidContext.UI.showError)
            .finally(this.ui.contentLoading.hide.bind(this.ui.contentLoading));
    }
};

/**
 * Callback function for content HTML document retrieval.
 */
HelpApp.prototype._callbackContent = function (data) {
    if (typeof(data) == "string") {
        MochiKit.DOM.setNodeAttribute(this.ui.contentLink, "href", this._currentUrl);
        this.ui.contentLink.className = "";
        this._showContentHtml(data);
        if (/#.+/.test(this._currentUrl)) {
            this._scrollLink(this._currentUrl.replace(/.*#/, ""));
        } else {
            this.ui.contentScroll.scrollTop = 0;
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
HelpApp.prototype._showContentHtml = function (html) {
    html = html.replace(/^[\s\S]*<body[^>]*>/i, "");
    html = html.replace(/<\/body>[\s\S]*$/i, "");
    html = html.replace(/^[\s\S]*<!--START-->/, "");
    html = html.replace(/<!--END-->[\s\S]*$/, "");
    html = html.replace(/^[\s\S]*(<div class="document">)/i, "$1");
    this.ui.contentText.innerHTML = html;
    var baseUrl = RapidContext.Util.resolveURI("");
    var links = this.ui.contentText.getElementsByTagName("a");
    for (var i = 0; i < links.length; i++) {
        var href = links[i].getAttribute("href");
        if (href && href != "") {
            href = RapidContext.Util.resolveURI(href, this._currentUrl);
            if (href.startsWith(baseUrl)) {
                href = href.substring(baseUrl.length);
            }
            if (links[i].hasAttribute("target")) {
                links[i].setAttribute("target", "doc");
            }
            if (href.includes("://")) {
                links[i].setAttribute("target", "doc");
            } else {
                links[i].setAttribute("href", href);
            }
        }
    }
    var images = this.ui.contentText.getElementsByTagName("img");
    for (var j = 0; j < images.length; j++) {
        var src = images[j].getAttribute("src");
        if (src) {
            src = RapidContext.Util.resolveURI(src, this._currentUrl);
            if (src.startsWith(baseUrl)) {
                src = src.substring(baseUrl.length);
            }
            if (!src.includes("://")) {
                images[j].setAttribute("src", src);
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
        if (href.startsWith(baseUrl)) {
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
        };
        this.ui.contentLocator.animate(opts);
    }
};
