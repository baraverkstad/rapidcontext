/**
 * Creates a new applet instance.
 */
function HelpApplet() {
    this._current = null;
    this._scrollName = null;
}

/**
 * Starts the applet and initializes the UI.
 */
HelpApplet.prototype.start = function() {
    this.clearContent();
    MochiKit.Signal.connect(this.ui.topicReload, "onclick", this, "loadTopics");
    MochiKit.Signal.connect(this.ui.topicTree, "onselect", this, "loadContent");
    MochiKit.Signal.connect(this.ui.contentReload, "onclick", this, "loadContent");
    MochiKit.Signal.connect(this.ui.contentExpand, "onclick", this, "_openWindow");
    this.loadTopics();
}

/**
 * Stops the applet.
 */
HelpApplet.prototype.stop = function() {
    // Nothing to do here
}

/**
 * Loads all available topics and displays them in the topic tree
 * view. Currently this method does not fetch any topic list from
 * the server, but only unifies the topic data from applets and the
 * constant platform docs.
 */
HelpApplet.prototype.loadTopics = function() {
    this.ui.topicTree.removeAll();
    var apps = RapidContext.App.applets();
    for (var i = 0; i < apps.length; i++) {
        for (var j = 0; j < apps[i].resources.length; j++) {
            var res = apps[i].resources[j];
            if (res.topic != null) {
                var data = MochiKit.Base.clone(res);
                data.source = apps[i].name + " (Applet)";
                this._addTopic(data);
            }
        }
    }
    for (var i = 0; i < HelpApplet.TOPICS.length; i++) {
        this._addTopic(HelpApplet.TOPICS[i]);
    }
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
 * Adds a topic data object to the topic tree.
 *
 * @param {Object} data the topic data object
 */
HelpApplet.prototype._addTopic = function(data) {
    var path = data.topic.split("/");
    var node = this.ui.topicTree.addPath(path);
    node.data = data;
}

/**
 * Finds a topic based on a specified URL. The search will be
 * performed in depth-first order through the tree.
 *
 * @param {Array} nodes the tree nodes to search
 * @param {String} url the URL to search for
 */
HelpApplet.prototype.findTopicByUrl = function(nodes, url) {
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
HelpApplet.prototype.clearContent = function() {
    this._current = null;
    this.ui.contentReload.hide();
    this.ui.contentLoading.hide();
    this.ui.contentExpand.hide();
    MochiKit.DOM.replaceChildNodes(this.ui.contentTitle);
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
HelpApplet.prototype.loadContent = function(url) {
    this.clearContent();
    if (typeof(url) != "string") {
        url = null;
    } else {
        var node = this.findTopicByUrl(this.ui.topicTree.getChildNodes(), url);
        if (node != null) {
            node.expand();
            node.select();
            var pos = url.indexOf("#");
            if (pos > 0) {
                this._scrollName = url.substring(pos + 1);
            }
            return false;
        }
    }
    var node = this.ui.topicTree.selectedChild();
    if (node != null) {
        var str = (node.isFolder()) ? "folderIcon" : "topicIcon";
        this.ui.contentIcon.src = this.resource[str];
        MochiKit.DOM.replaceChildNodes(this.ui.contentTitle, node.name);
        if (node.data != null) {
            this.ui.contentLoading.show();
            str = "Static document from " + node.data.source
            MochiKit.DOM.replaceChildNodes(this.ui.contentInfo, str);
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
HelpApplet.prototype._callbackContent = function(data) {
    this.ui.contentReload.show();
    this.ui.contentLoading.hide();
    if (data instanceof Error) {
        RapidContext.UI.showError(data);
    } else if (typeof(data) == "string") {
        this._current.text = data;
        this.ui.contentExpand.show();
        this._showContentHtml(data);
        if (this._scrollName != null) {
            this._scrollLink(this._scrollName);
            this._scrollName = null;
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
HelpApplet.prototype._showContentHtml = function(html) {
    var m = html.match(/<body[^>]*>([\s\S]*)<\/body>/im);
    if (m != null) {
        html = m[1];
    }
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
HelpApplet.prototype._scrollLink = function(name) {
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

/**
 * Opens a new window with the contents of the content pane. This is
 * primarily useful for printing documents.
 */
HelpApplet.prototype._openWindow = function() {
    var node = this.ui.topicTree.selectedChild();
    var win = window.open("", "_blank");
    var doc = win.document;
    // TODO: clean up this HTML generation...
    doc.open();
    doc.write("<?xml version='1.0' encoding='UTF-8'?>\n");
    doc.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
              "\"DTD/xhtml1-strict.dtd\">\n");
    doc.write("<html xmlns='http://www.w3.org/1999/xhtml'>\n");
    doc.write("<head>\n");
    doc.write("<link rel='icon' type='image/gif' href='images/favicon.gif' />\n");
    doc.write("<link rel='stylesheet' href='css/style.css' type='text/css' />\n");
    doc.write("<title>Help &amp; Docs :: " + node.name + "</title>\n");
    doc.write("</head>\n");
    doc.write("<body style='width: auto; height: auto; padding-top: 15px; padding-bottom: 15px; " +
              "padding-left: 30px; padding-right: 30px;'>\n");
    doc.write(this.ui.contentScroll.innerHTML);
    doc.write("</body>\n</html>\n");
    doc.close();
}
