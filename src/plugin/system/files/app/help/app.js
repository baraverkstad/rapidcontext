class HelpApp {

    constructor() {
        this._topics = null;
        this._topicUrls = null;
        this._currentUrl = "";
    }

    /**
     * Starts the app and initializes the UI.
     */
    start() {
        this.ui.topicReload.on("click", () => this.loadTopics());
        this.ui.topicTree.on("expand", (evt) => this._treeOnExpand(evt));
        this.ui.topicTree.on("select", () => this._treeOnSelect());
        RapidContext.UI.Event.on(this.ui.contentText, "click", (evt) => this._handleClick(evt));
        this.loadTopics();
    }

    /**
     * Stops the app.
     */
    stop() {
        // Nothing to do here
    }

    /**
     * Loads all available topics and displays them in the topic tree
     * view. Currently this method does not fetch new topic data from
     * the server, but only unifies the topic data from apps and the
     * previously loaded platform topics.
     */
    loadTopics() {
        const root = this._topics = { path: [], child: {}, children: [] };
        const topicUrls = this._topicUrls = {};

        // Add a topic path to a parent (and its children)
        function addPath(parent, path) {
            while (path.length > 0) {
                const name = path.shift();
                let child = parent.child[name];
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
            const topic = addPath(parent, obj.topic.split("/"));
            if (topic.source) {
                console.warn("Duplicated Help topic, possibly overwritten", obj.topic);
            }
            topic.source = obj.source ?? source;
            if (obj.url) {
                topic.url = obj.url;
                topicUrls[obj.url] = topic;
            }
            if (obj.external) {
                topic.url = new URL(obj.url, document.baseURI).toString();
            }
            addAll(topic, topic.source, obj.children);
        }

        // Add a list of help topics
        function addAll(parent, source, obj) {
            if (Array.isArray(obj)) {
                obj.forEach((item) => addAll(parent, source, item));
            } else if (obj && typeof(obj.topic) == "string") {
                add(parent, source, obj);
            }
        }

        // Add app topics
        const apps = RapidContext.App.apps();
        for (let i = 0; i < apps.length; i++) {
            addAll(root, `${apps[i].name} (App)`, apps[i].resources);
        }

        // Add platform topics
        const source = "RapidContext Platform Documentation";
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
    }

    /**
     * Adds the child topics to a node.
     *
     * @param {Tree/TreeNode} parentNode the parent tree or tree node
     * @param {Object} topic the parent topic
     */
    _treeInsertChildren(parentNode, topic) {
        for (let i = 0; i < topic.children.length; i++) {
            const child = topic.children[i];
            const attrs = {
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
            const node = RapidContext.Widget.TreeNode(attrs);
            node.data = child;
            parentNode.addAll(node);
        }
    }

    /**
     * Expands the tree topic corresponding to a specified URL. The URL
     * must exactly match a topic URL, or no match will be found.
     *
     * @param {string} url the URL to search for
     *
     * @return {TreeNode} the TreeNode widget for the matching topic
     */
    _treeExpandUrl(url) {
        if (this._treeBlockEvents) {
            return this.ui.topicTree.selectedChild();
        } else {
            const topic = this._topicUrls[url] || this._topicUrls[url.replace(/#.*/, "")];
            if (topic) {
                const path = topic.path;
                for (let i = 0; i < path.length; i++) {
                    this.ui.topicTree.findByPath(path.slice(0, i + 1)).expand();
                }
                const node = this.ui.topicTree.findByPath(path);
                this._treeBlockEvents = true;
                node.select();
                this._treeBlockEvents = false;
                return node;
            }
        }
        return null;
    }

    /**
     * Handles the tree expand and collapse events.
     *
     * @param {Event} evt the tree node expand event
     */
    _treeOnExpand(evt) {
        const node = evt.detail.node;
        const topic = node.data;
        if (node.isExpanded && topic.children.length != node.getChildNodes().length) {
            this._treeInsertChildren(node, topic);
        }
    }

    /**
     * Handles the tree select events.
     */
    _treeOnSelect() {
        if (!this._treeBlockEvents) {
            const node = this.ui.topicTree.selectedChild();
            if (node?.data?.url) {
                this._treeBlockEvents = true;
                this.loadContent(node.data.url);
                this._treeBlockEvents = false;
            }
        }
    }

    /**
     * Clears the content view from any loaded topic data.
     */
    clearContent() {
        this._currentUrl = "";
        this.ui.contentLoading.hide();
        this.ui.contentLink.classList.add("hidden");
        this.ui.contentInfo.innerHTML = "";
        this.ui.contentText.innerHTML = "";
    }

    /**
     * Loads a new HTML document into the content view. The document is
     * loaded from a specified URL and the tree is updated to select the
     * topic matching the URL (if any). Note that for external URL:s, a
     * separate window (tab) is opened instead.
     *
     * @param {string} url the content URL (HTML document)
     */
    loadContent(url) {
        const node = this._treeExpandUrl(url);
        const fileUrl = url.replace(/#.*/, "");
        if (/^https?:/.test(url)) {
            window.open(url);
        } else if (url.includes("#") && this._currentUrl.startsWith(fileUrl)) {
            this._currentUrl = url;
            this._scrollLink(url.replace(/.*#/, ""));
        } else {
            this.clearContent();
            this._currentUrl = url;
            this.ui.contentLoading.show();
            const source = node?.data?.source ?? "";
            this.ui.contentInfo.innerText = source;
            RapidContext.App.loadText(fileUrl)
                .then((data) => this._callbackContent(data))
                .catch(RapidContext.UI.showError)
                .finally(() => this.ui.contentLoading.hide());
        }
    }

    /**
     * Callback function for content HTML document retrieval.
     */
    _callbackContent(data) {
        if (typeof(data) == "string") {
            this.ui.contentLink.setAttribute("href", this._currentUrl);
            this.ui.contentLink.classList.remove("hidden");
            this._showContentHtml(data);
            if (/#.+/.test(this._currentUrl)) {
                this._scrollLink(this._currentUrl.replace(/.*#/, ""));
            } else {
                this.ui.contentScroll.scrollTop = 0;
            }
        } else {
            this.ui.contentInfo.innerText = "Not Found";
        }
    }

    /**
     * Displays content HTML data. This method replaces all links in the
     * HTML document with script handlers to be able to open new
     * documents either in new windows or in the same content pane.
     *
     * @param {string} html the HTML data to display
     */
    _showContentHtml(html) {
        html = html.replace(/^[\s\S]*<body[^>]*>/i, "");
        html = html.replace(/<\/body>[\s\S]*$/i, "");
        html = html.replace(/^[\s\S]*<!--START-->/, "");
        html = html.replace(/<!--END-->[\s\S]*$/, "");
        html = html.replace(/^[\s\S]*(<div class="document">)/i, "$1");
        this.ui.contentText.innerHTML = html;
        const base = document.baseURI.replace(/[^/]+$/, "");
        const current = new URL(this._currentUrl, base);
        this.ui.contentText.querySelectorAll("a").forEach((el) => {
            let href = el.getAttribute("href");
            if (href) {
                href = new URL(href, current).toString();
                if (href.startsWith(base)) {
                    href = href.substring(base.length);
                    el.setAttribute("href", href);
                }
                if (href.includes("://") || el.hasAttribute("target")) {
                    el.setAttribute("target", "doc");
                }
            }
        });
        this.ui.contentText.querySelectorAll("img").forEach((el) => {
            let src = el.getAttribute("src");
            if (src) {
                src = new URL(src, current).toString();
                if (src.startsWith(base)) {
                    src = src.substring(base.length);
                    el.setAttribute("src", src);
                }
            }
        });
    }

    /**
     * Handles click events in the content text.
     */
    _handleClick(evt) {
        const elem = evt.target.closest("a");
        if (elem && elem.hasAttribute("href") && !elem.hasAttribute("target")) {
            evt.preventDefault();
            evt.stopImmediatePropagation();
            let href = elem.getAttribute("href");
            const base = document.baseURI.replace(/[^/]+$/, "");
            if (href.startsWith(base)) {
                href = href.substring(base.length);
            }
            this.loadContent(href);
        }
    }

    /**
     * Scrolls the content pane to make the specified link name visible.
     *
     * @param {string} name the link name attribute
     */
    _scrollLink(name) {
        const selector = `a[name='${name}'], *[id='${name}']`;
        const ctx = $(this.ui.contentText).find(selector);
        if (ctx.length) {
            const elem = ctx[0];
            this.ui.contentScroll.scrollTop = elem.offsetTop;
            this.ui.contentLocator.classList.remove("hidden", "-fade-out");
            this.ui.contentLocator.style.top = `${elem.offsetTop}px`;
            this.ui.contentLocator.classList.add("-fade-out");
        }
    }
}

// FIXME: Switch to module and export class instead
window.HelpApp = HelpApp;
