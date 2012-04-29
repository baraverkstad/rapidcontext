/**
 * Creates a new app instance.
 */
function HelpApp() {
    this._topics = { child: {}, children: [] };
    this._current = null;
}

/**
 * Starts the app and initializes the UI.
 */
HelpApp.prototype.start = function() {
    this.clearContent();
    MochiKit.Signal.connect(this.ui.topicReload, "onclick", this, "loadTopics");
    MochiKit.Signal.connect(this.ui.topicTree, "onexpand", this, "_expandTopic");
    MochiKit.Signal.connect(this.ui.topicTree, "onselect", this, "loadContent");
    this.loadTopics();
}

/**
 * Stops the app.
 */
HelpApp.prototype.stop = function() {
    // Nothing to do here
}

/**
 * Loads all available topics and displays them in the topic tree
 * view. Currently this method does not fetch new topic data from
 * the server, but only unifies the topic data from apps and the
 * previously loaded platform topics.
 */
HelpApp.prototype.loadTopics = function() {
    this._topics = { child: {}, children: [] };
    this.ui.topicTree.removeAll();
    var apps = RapidContext.App.apps();
    for (var i = 0; i < apps.length; i++) {
        for (var j = 0; j < apps[i].resources.length; j++) {
            var res = apps[i].resources[j];
            if (res.topic != null) {
                var data = MochiKit.Base.clone(res);
                data.source = apps[i].name + " (App)";
                this._addTopic(data);
            }
        }
    }
    var topics = [];
    MochiKit.Base.extend(topics, this.resource.topicsBase);
    MochiKit.Base.extend(topics, this.resource.topicsJsApi);
    MochiKit.Base.extend(topics, this.resource.topicsMochiKit);
    MochiKit.Base.extend(topics, this.resource.topicsExtra);
    for (var i = 0; i < topics.length; i++) {
        var topic = topics[i];
        topic.source = "RapidContext Platform Documentation";
        this._addTopic(topic);
    }
    this._insertTopic(this.ui.topicTree, this._topics);
    this.ui.topicTree.expandAll(1);
    if (this._current != null) {
        var path = this._current.topic.split("/");
        var node = this.ui.topicTree.findByPath(path);
        if (node != null) {
            node.expand();
            node.select();
        }
    }
}

/**
 * Adds a topic data object to the internal topic tree structure.
 *
 * @param {Object} topic the topic data object
 */
HelpApp.prototype._addTopic = function (topic) {
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
        MochiKit.Base.update(parent.child[name], topic);
    } else {
        MochiKit.Base.update(topic, { name: name, child: {}, children: [] });
        parent.child[name] = topic;
        parent.children.push(topic);
    }
}

/**
 * Adds the child topics to a node if it is expanded and the nodes
 * have not already been added.
 *
 * @param {TreeNode} node the tree node to add to
 */
HelpApp.prototype._expandTopic = function (node) {
    var topic = node.data;
    if (node.isExpanded && topic.children.length != node.getChildNodes().length) {
        this._insertTopic(node, topic);
    }
}

/**
 * Adds the child topics to a node.
 *
 * @param {Tree/TreeNode} parentNode the parent tree or tree node
 * @param {Object} topic the parent topic
 */
HelpApp.prototype._insertTopic = function (parentNode, topic) {
    for (var i = 0; i < topic.children.length; i++) {
        var child = topic.children[i];
        var attrs = {
            name: child.name,
            folder: (child.children.length > 0),
            icon: (!child.url ? "FOLDER" : /#/.test(child.url) ? "TAG_BLUE" : "BOOK_OPEN")
        };
        var node = RapidContext.Widget.TreeNode(attrs);
        node.data = child;
        parentNode.addAll(node);
    }
}

/**
 * Finds a topic based on a specified URL. The search will be
 * performed in depth-first order through the tree.
 *
 * @param {Array} nodes the tree nodes to search
 * @param {String} url the URL to search for
 */
HelpApp.prototype.findTopicByUrl = function(nodes, url) {
    // TODO: use topic tree structure instead...
    for (var i = 0; nodes != null && i < nodes.length; i++) {
        var node = nodes[i];
        if (node.data != null && url.indexOf(node.data.url) == 0) {
            return node;
        }
        node = this.findTopicByUrl(node.getChildNodes(), url);
        if (node != null) {
            return node;
        }
    }
    return null;
}

/**
 * Clears the content view from any loaded topic data.
 */
HelpApp.prototype.clearContent = function() {
    this._current = null;
    this.ui.contentLoading.hide();
    this.ui.contentExpand.className = "hidden";
    MochiKit.DOM.replaceChildNodes(this.ui.contentInfo);
    MochiKit.DOM.replaceChildNodes(this.ui.contentText);
}

/**
 * Loads new content into the content view. The content is either
 * loaded from a specified URL or from the currently selected topic
 * tree node.
 *
 * @param {String} [url] the optional content data URL
 */
HelpApp.prototype.loadContent = function (url) {
    this.clearContent();
    if (typeof(url) != "string") {
        url = null;
    } else {
        var node = this.findTopicByUrl(this.ui.topicTree.getChildNodes(), url);
        if (node != null) {
            node.expand();
            node.select();
            return false;
        }
    }
    var node = this.ui.topicTree.selectedChild();
    if (node != null) {
        if (node.data && node.data.url) {
            this.ui.contentLoading.show();
            MochiKit.DOM.replaceChildNodes(this.ui.contentInfo, node.data.source);
            url = url || node.data.url;
            this._current = { topic: node.data.topic, text: null, url: url };
            var d = RapidContext.App.loadText(url, null, { timeout: 60 });
            d.addBoth(MochiKit.Base.bind("_callbackContent", this));
        }
    }
    return false;
}

/**
 * Callback function for content HTML document retrieval.
 */
HelpApp.prototype._callbackContent = function(data) {
    this.ui.contentLoading.hide();
    if (data instanceof Error) {
        RapidContext.UI.showError(data);
    } else if (typeof(data) == "string") {
        this._current.text = data;
        MochiKit.DOM.setNodeAttribute(this.ui.contentExpand, "href", this._current.url);
        this.ui.contentExpand.className = "";
        this._showContentHtml(data);
        if (/#.+/.test(this._current.url)) {
            this._scrollLink(this._current.url.replace(/.*#/, ""));
        }
    } else {
        MochiKit.DOM.replaceChildNodes(this.ui.contentInfo, "Not Found");
    }
/* TODO: re-enable display when readable from data store
        this._ui.info.setText("Revision " + data.revisions +
                              ", Last modified " + data.modified_dttm +
                              " by " + data.modified_oprid);
        this._showContentHtml(data.text);
*/
}

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
    var baseUrl = window.location.href;
    var nodes = this.ui.contentText.getElementsByTagName("a");
    nodes = MochiKit.Base.extend([], nodes);
    for (var i = 0; i < nodes.length; i++) {
        var href = nodes[i].getAttribute("href");
        if (href != null && href.indexOf(baseUrl) == 0) {
            // Patch for IE, since it returns absolute href values...
            href = href.substring(baseUrl.length);
        }
        if (href == null || href == "") {
            // Ignore missing or blank links
        } else if (href.indexOf("#") == 0) {
            href = href.substring(1);
            nodes[i].href = this._current.url + "#" + href;
            nodes[i].onclick = MochiKit.Base.bind("_scrollLink", this, href);
        } else if (href.indexOf("://") > 0) {
            nodes[i].target = "_blank";
        } else {
            href = RapidContext.Util.resolveURI(href, this._current.url);
            nodes[i].href = href;
            nodes[i].onclick = MochiKit.Base.bind("loadContent", this, href);
        }
    }
    var nodes = this.ui.contentText.getElementsByTagName("img");
    nodes = MochiKit.Base.extend([], nodes);
    for (var i = 0; i < nodes.length; i++) {
        var href = nodes[i].getAttribute("src");
        if (href != null && href.indexOf(baseUrl) == 0) {
            // Patch for IE, since it returns absolute href values...
            href = href.substring(baseUrl.length);
        }
        if (href == null || href == "" || href.indexOf("://") > 0) {
            // Ignore missing, blank or absolute URL:s
        } else {
            nodes[i].src = RapidContext.Util.resolveURI(href, this._current.url);
        }
    }
}

/**
 * Scrolls the content pane to make the specified link name visible.
 *
 * @param {String} name the link name attribute
 */
HelpApp.prototype._scrollLink = function(name) {
    var nodes = this.ui.contentText.getElementsByTagName("a");
    for (var i = 0; i < nodes.length; i++) {
        if (nodes[i].name == name) {
            var parentNode = this.ui.contentScroll;
            var parentPos = MochiKit.Style.getElementPosition(parentNode);
            var pos = MochiKit.Style.getElementPosition(nodes[i], parentPos);
            RapidContext.Util.setScrollOffset(parentNode, pos);
            break;
        }
    }
    return false;
}
