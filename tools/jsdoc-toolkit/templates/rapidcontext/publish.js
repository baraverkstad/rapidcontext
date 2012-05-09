/** Called automatically by JsDoc Toolkit. */
function publish(symbolSet) {
    publish.conf = {  // trailing slash expected for dirs
        ext:         ".html",
        outDir:      JSDOC.opt.d || SYS.pwd+"../out/jsdoc/",
        templatesDir: JSDOC.opt.t || SYS.pwd+"../templates/jsdoc/",
        symbolsDir:  "",
        srcDir:      "src/"
    };

    // is source output is suppressed, just display the links to the source file
    if (JSDOC.opt.s && defined(Link) && Link.prototype._makeSrcLink) {
        Link.prototype._makeSrcLink = function(srcFilePath) {
            return "&lt;"+srcFilePath+"&gt;";
        }
    }

    // create the folders and subfolders to hold the output
    IO.mkPath((publish.conf.outDir+publish.conf.srcDir).split("/"));

    // used to allow Link to check the details of things being linked to
    Link.symbolSet = symbolSet;

    // create the required templates
    try {
        var classTemplate = new JSDOC.JsPlate(publish.conf.templatesDir+"class.tmpl");
    }
    catch(e) {
        print("Couldn't create the required templates: "+e);
        quit();
    }

    // some utility filters
    function hasNoParent($) {return ($.memberOf == "")}
    function isaFile($) {return ($.is("FILE"))}
    function isaClass($) {return ($.is("CONSTRUCTOR") || $.isNamespace) && /^RapidContext\./.test($.alias);}

    // get an array version of the symbolset, useful for filtering
    var symbols = symbolSet.toArray();

    // create the hilited source code files
    var files = JSDOC.opt.srcFiles;
     for (var i = 0, l = files.length; i < l; i++) {
         var file = files[i];
         var srcDir = publish.conf.outDir + publish.conf.srcDir;
        makeSrcFile(file, srcDir);
     }

     // get a list of all the classes in the symbolset
     var classes = symbols.filter(isaClass).sort(makeSortby("alias"));

    // create a filemap in which outfiles must be to be named uniquely, ignoring case
    if (JSDOC.opt.u) {
        var filemapCounts = {};
        Link.filemap = {};
        for (var i = 0, l = classes.length; i < l; i++) {
            var lcAlias = classes[i].alias.toLowerCase();

            if (!filemapCounts[lcAlias]) filemapCounts[lcAlias] = 1;
            else filemapCounts[lcAlias]++;

            Link.filemap[classes[i].alias] =
                (filemapCounts[lcAlias] > 1)?
                lcAlias+"_"+filemapCounts[lcAlias] : lcAlias;
        }
    }

    // create each of the class pages
    for (var i = 0, l = classes.length; i < l; i++) {
        var symbol = classes[i];

        symbol.events = symbol.getEvents();   // 1 order matters
        symbol.methods = symbol.getMethods(); // 2

        Link.currentSymbol= symbol;
        var output = "";
        output = classTemplate.process(symbol);

        IO.saveFile(publish.conf.outDir+publish.conf.symbolsDir, ((JSDOC.opt.u)? Link.filemap[symbol.alias] : symbol.alias) + publish.conf.ext, output);
    }

    // create the class index page and symbol index file
    try {
        var classIndexTemplate = new JSDOC.JsPlate(publish.conf.templatesDir+"index.tmpl");
        var topicsTemplate = new JSDOC.JsPlate(publish.conf.templatesDir+"topics.tmpl");
    } catch(e) { print(e.message); quit(); }
    IO.saveFile(publish.conf.outDir, "index"+publish.conf.ext, classIndexTemplate.process(classes));
    IO.saveFile(publish.conf.outDir, "topics.json", topicsTemplate.process(classes));
}

function html(str) {
    if (str == null) return "";
    str = str.toString();
    str = str.replace(/&/g, "&amp;");
    str = str.replace(/==>/g, "&rArr;");
    str = str.replace(/</g, "&lt;");
    str = str.replace(/>/g, "&gt;");
    return str;
}

/** Just the first sentence (up to a full stop). Should not break on dotted variable names. */
function summarize(desc) {
    if (typeof desc != "undefined")
        return desc.match(/([\w\W]+?\.)[^a-z0-9_$]/i)? RegExp.$1 : desc;
}

/** Make a symbol sorter by some attribute. */
function makeSortby(attribute) {
    return function(a, b) {
        if (a[attribute] != undefined && b[attribute] != undefined) {
            a = a[attribute];
            b = b[attribute];
            if (a < b) return -1;
            if (a > b) return 1;
            return 0;
        }
    }
}

function isOwnProperty(parent) {
    return function ($) {
        return $.memberOf == parent.alias && !$.isNamespace;
    }
}

function ownProperties(list, parent) {
    return list.filter(isOwnProperty(parent)).sort(makeSortby("name"));
}

function allOwnProperties(parent) {
    var list = [];
    list.push.apply(list, parent.methods);
    list.push.apply(list, parent.properties);
    list.push.apply(list, parent.events);
    return ownProperties(list, parent);
}

/** Pull in the contents of an external file at the given path. */
function include(path) {
    var path = publish.conf.templatesDir+path;
    return IO.readFile(path);
}

/** Turn a raw source file into a code-hilited page in the docs. */
function makeSrcFile(path, srcDir, name) {
    if (JSDOC.opt.s) return;

    if (!name) {
        name = path.replace(/\.\.?[\\\/]/g, "").replace(/[\\\/]/g, "_");
        name = name.replace(/\:/g, "_");
    }

    var src = {path: path, name:name, charset: IO.encoding, hilited: ""};

    if (defined(JSDOC.PluginManager)) {
        JSDOC.PluginManager.run("onPublishSrc", src);
    }

    if (src.hilited) {
        IO.saveFile(srcDir, name+publish.conf.ext, src.hilited);
    }
}

/** Build output for displaying function parameters. */
function makeSignature(params) {
    if (!params) return "()";
    var signature = "("
    +
    params.filter(
        function($) {
            return $.name.indexOf(".") == -1; // don't show config params in signature
        }
    ).map(
        function($) {
            return $.name;
        }
    ).join(", ")
    +
    ")";
    return signature;
}

/** Find symbol {@link ...} strings in text and turn into html links */
function resolveLinks(str, from) {
    str = html(str);
    str = str.replace(/(\n|\r\n|\r){2,}/g, "<br/><br/>");
    str = str.replace(/`([^`\n]+)`/g, "<var>$1</var>");
    str = str.replace(/"([^"\n]+)"/g, "<q>$1</q>");
    str = str.replace(/'([^'\n]+)'/g, "<q>$1</q>");
    str = str.replace(/\{@link ([^} ]+) ?\}/gi,
        function(match, symbolName) {
            return new Link().toSymbol(symbolName);
        }
    );
    return str;
}
