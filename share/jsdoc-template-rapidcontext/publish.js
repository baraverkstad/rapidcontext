/* eslint stylistic/quotes: ['error', 'single'] */
/* global require, exports */
const doop = require('jsdoc/util/doop');
const env = require('jsdoc/env');
const fs = require('jsdoc/fs');
const helper = require('jsdoc/util/templateHelper');
const logger = require('jsdoc/util/logger');
const path = require('jsdoc/path');
const { taffy } = require('@jsdoc/salty');
const template = require('jsdoc/template');
const util = require('util');
const hljs = require('highlight.js/lib/common');

const linkto = helper.linkto;
const resolveAuthorLinks = helper.resolveAuthorLinks;

let data;
let view;

let outdir = path.normalize(env.opts.destination);

function find(spec) {
    return helper.find(data, spec);
}

function upperFirst(s) {
    return s.charAt(0).toUpperCase() + s.substring(1);
}

function htmlsafe(text) {
    return String(text || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('\'', '&apos;')
        .replaceAll('"', '&quot;');
}

function htmltext(text, inline) {
    const s = htmlsafe(text).replaceAll(/`(.*?)`/g, (m, g) => `<code>${g}</code>`);
    return inline ? s : s.split(/\n\n+/g).map((s) => `<p>${s}</p>`).join('\n\n');
}

function htmllist(items) {
    if (Array.isArray(items) && items.length > 1) {
        return `<ul>${items.map((s) => `<li>${s}</li>`)}</ul>`;
    } else {
        return items[0] || items;
    }
}

function summarize(text) {
    const s = (text || '').split(/\n\n|\.\s/g)[0].trim();
    return htmltext(s.endsWith('.') ? s : `${s}.`, true);
}

function highlight(code, language) {
    if (language) {
        return hljs.highlight(code, { language }).value;
    } else {
        return hljs.highlightAuto(code).value;
    }
}

function typeLabel({ kind }) {
    const shortname = {
        namespace: 'NS',
        class: 'C',
        interface: 'I',
        method: 'M',
        mixin: 'MI',
    };
    return `<span class="label-${kind}" title="${upperFirst(kind)}">${shortname[kind] || kind}</span>`;
}

function tutoriallink(tutorial) {
    return helper.toTutorial(tutorial, null, {
        tag: 'em',
        classname: 'disabled',
        prefix: 'Tutorial: '
    });
}

function getAncestorLinks(doclet) {
    return helper.getAncestorLinks(data, doclet);
}

function hashToLink(doclet, hash) {
    let url;

    if ( !/^(#.+)/.test(hash) ) {
        return hash;
    }

    url = helper.createLink(doclet);
    url = url.replace(/(#.+|$)/, hash);

    return `<a href="${url}">${hash}</a>`;
}

function needsSignature({ kind, type, meta }) {
    let needsSig = false;
    if (kind === 'function' || kind === 'class') {
        // function and class definitions always get a signature
        needsSig = true;
    } else if (kind === 'typedef' && type && type.names && type.names.length) {
        // typedefs that contain functions get a signature, too
        for (let i = 0, l = type.names.length; i < l; i++) {
            if (type.names[i].toLowerCase() === 'function') {
                needsSig = true;
                break;
            }
        }
    } else if (kind === 'namespace' && meta && meta.code && meta.code.type && meta.code.type.match(/[Ff]unction/)) {
        // and namespaces that are functions get a signature (but finding them is a bit messy)
        needsSig = true;
    }
    return needsSig;
}

function getSignatureAttributes({ optional, nullable }) {
    const attributes = [];

    if (optional) {
        attributes.push('opt');
    }

    if (nullable === true) {
        attributes.push('nullable');
    } else if (nullable === false) {
        attributes.push('non-null');
    }

    return attributes;
}

function updateItemName(item) {
    const attributes = getSignatureAttributes(item);
    let itemName = item.name || '';

    if (item.variable) {
        itemName = `&hellip;${itemName}`;
    }

    if (attributes && attributes.length) {
        itemName = util.format( '%s<span class="signature-attributes">%s</span>', itemName,
            attributes.join(', ') );
    }

    return itemName;
}

function addParamAttributes(params) {
    return params.filter(({ name }) => name && !name.includes('.')).map(updateItemName);
}

function buildItemTypeStrings(item) {
    const types = [];

    if (item && item.type && item.type.names) {
        item.type.names.forEach(name => {
            types.push( linkto(name, htmlsafe(name)) );
        });
    }

    return types;
}

function buildAttribsString(attribs) {
    let attribsString = '';

    if (attribs && attribs.length) {
        attribsString = htmlsafe( util.format('(%s) ', attribs.join(', ')) );
    }

    return attribsString;
}

function addNonParamAttributes(items) {
    let types = [];

    items.forEach(item => {
        types = types.concat( buildItemTypeStrings(item) );
    });

    return types;
}

function addSignatureParams(f) {
    const params = f.params ? addParamAttributes(f.params) : [];

    f.signature = util.format( '%s(%s)', (f.signature || ''), params.join(', ') );
}

function addSignatureReturns(f) {
    const attribs = [];
    let attribsString = '';
    let returnTypes = [];
    let returnTypesString = '';
    const source = f.yields || f.returns;

    // jam all the return-type attributes into an array. this could create odd results (for example,
    // if there are both nullable and non-nullable return types), but let's assume that most people
    // who use multiple @return tags aren't using Closure Compiler type annotations, and vice-versa.
    if (source) {
        source.forEach(item => {
            helper.getAttribs(item).forEach(attrib => {
                if (!attribs.includes(attrib)) {
                    attribs.push(attrib);
                }
            });
        });

        attribsString = buildAttribsString(attribs);
    }

    if (source) {
        returnTypes = addNonParamAttributes(source);
    }
    if (returnTypes.length) {
        returnTypesString = util.format( ' &rarr; %s{%s}', attribsString, returnTypes.join('|') );
    }

    f.signature = [
        `<span class="signature">${f.signature || ''}</span>`,
        `<span class="type-signature">${returnTypesString}</span>`
    ].join('');
}

function addSignatureTypes(f) {
    const types = f.type ? buildItemTypeStrings(f) : [];
    f.signature =
        `${f.signature || ''}<span class="type-signature">${types.length ? ` :${types.join('|')}` : ''}</span>`;
}

function addAttribs(f) {
    const attribs = helper.getAttribs(f);
    const attribsString = buildAttribsString(attribs);
    f.attribs = util.format('<span class="type-signature">%s</span>', attribsString);
}

function shortenPaths(files, commonPrefix) {
    Object.keys(files).forEach(file => {
        files[file].shortened = files[file].resolved.replace(commonPrefix, '')
            // always use forward slashes
            .replace(/\\/g, '/');
    });

    return files;
}

function getPathFromDoclet({ meta }) {
    if (!meta) {
        return null;
    }

    return meta.path && meta.path !== 'null'
        ? path.join(meta.path, meta.filename)
        : meta.filename;
}

function generate(title, docs, filename, resolveLinks) {
    resolveLinks = resolveLinks !== false;
    const docData = {
        env: env,
        title: title,
        docs: docs
    };
    const outpath = path.join(outdir, filename);
    let html = view.render('container.tmpl', docData);
    if (resolveLinks) {
        html = helper.resolveLinks(html); // turn {@link foo} into <a href="foodoc.html">foo</a>
    }
    fs.writeFileSync(outpath, html, 'utf8');
}

function generateSourceFiles(sourceFiles, encoding = 'utf8') {
    function lineno(line, idx) {
        return `<span id="line${idx + 1}" class="lineno">${idx + 1}</span>${line}`;
    }
    Object.keys(sourceFiles).forEach(file => {
        const fileName = sourceFiles[file].shortened;
        // links are keyed to the shortened path in each doclet's `meta.shortpath` property
        const outfile = helper.getUniqueFilename(fileName);
        helper.registerLink(fileName, outfile);
        try {
            const code = fs.readFileSync(sourceFiles[file].resolved, encoding);
            const html = highlight(code, 'js').split(/\n/g).map(lineno).join('\n');
            const source = { kind: 'source', name: fileName, code: html };
            generate(`Source ${fileName}`, [source], outfile, false);
        } catch (e) {
            logger.error('Error while generating source file %s: %s', file, e.message);
        }
    });
}

/**
 * Look for classes or functions with the same name as modules (which indicates that the module
 * exports only that class or function), then attach the classes or functions to the `module`
 * property of the appropriate module doclets. The name of each class or function is also updated
 * for display purposes. This function mutates the original arrays.
 *
 * @private
 * @param {Array.<module:jsdoc/doclet.Doclet>} doclets - The array of classes and functions to
 * check.
 * @param {Array.<module:jsdoc/doclet.Doclet>} modules - The array of module doclets to search.
 */
function attachModuleSymbols(doclets, modules) {
    const symbols = {};

    // build a lookup table
    doclets.forEach(symbol => {
        symbols[symbol.longname] = symbols[symbol.longname] || [];
        symbols[symbol.longname].push(symbol);
    });

    modules.forEach(module => {
        if (symbols[module.longname]) {
            module.modules = symbols[module.longname]
                // Only show symbols that have a description. Make an exception for classes, because
                // we want to show the constructor-signature heading no matter what.
                .filter(({ description, kind }) => description || kind === 'class')
                .map(symbol => {
                    symbol = doop(symbol);

                    if (symbol.kind === 'class' || symbol.kind === 'function') {
                        symbol.name = `${symbol.name.replace('module:', '(require("')}"))`;
                    }

                    return symbol;
                });
        }
    });
}

/**
    @param {TAFFY} taffyData See <http://taffydb.com/>.
    @param {object} opts
    @param {Tutorial} tutorials
 */
exports.publish = (taffyData, opts, tutorials) => {
    const sourceFilePaths = [];
    let sourceFiles = {};

    data = taffyData;

    const conf = env.conf.templates || {};
    conf.default = conf.default || {};

    const templatePath = path.normalize(opts.template);
    view = new template.Template( path.join(templatePath, 'tmpl') );

    // claim some special filenames in advance, so the All-Powerful Overseer of Filename Uniqueness
    // doesn't try to hand them out later
    const indexUrl = helper.getUniqueFilename('index');
    // don't call registerLink() on this one! 'index' is also a valid longname

    const globalUrl = helper.getUniqueFilename('global');
    helper.registerLink('global', globalUrl);

    // set up templating
    view.layout = conf.default.layoutFile
        ? path.getResourcePath(path.dirname(conf.default.layoutFile), path.basename(conf.default.layoutFile))
        : 'layout.tmpl';

    // set up tutorials for helper
    helper.setTutorials(tutorials);

    data = helper.prune(data);
    data.sort('longname, version, since');
    helper.addEventListeners(data);

    data().each(doclet => {
        let sourcePath;

        doclet.attribs = '';

        if (doclet.examples) {
            doclet.examples = doclet.examples.map(example => {
                let caption;
                let code;

                if (example.match(/^\s*<caption>([\s\S]+?)<\/caption>(\s*[\n\r])([\s\S]+)$/i)) {
                    caption = RegExp.$1;
                    code = RegExp.$3;
                }

                return {
                    caption: caption || '',
                    code: code || example
                };
            });
        }
        if (doclet.see) {
            doclet.see.forEach((seeItem, i) => {
                doclet.see[i] = hashToLink(doclet, seeItem);
            });
        }

        // build a list of source files
        if (doclet.meta) {
            sourcePath = getPathFromDoclet(doclet);
            sourceFiles[sourcePath] = {
                resolved: sourcePath,
                shortened: null
            };
            if (!sourceFilePaths.includes(sourcePath)) {
                sourceFilePaths.push(sourcePath);
            }
        }
    });

    // update outdir if necessary, then create outdir
    const packageInfo = ( find({ kind: 'package' }) || [] )[0];
    if (packageInfo && packageInfo.name) {
        outdir = path.join( outdir, packageInfo.name, (packageInfo.version || '') );
    }
    fs.mkPath(outdir);

    // copy the template's static files to outdir
    const fromDir = path.join(templatePath, 'static');
    const staticFiles = fs.statSync(fromDir, { throwIfNoEntry: false }) ? fs.ls(fromDir, 3) : [];
    staticFiles.forEach(fileName => {
        const toDir = fs.toDir( fileName.replace(fromDir, outdir) );

        fs.mkPath(toDir);
        fs.copyFileSync(fileName, toDir);
    });

    // copy user-specified static files to outdir
    if (conf.default.staticFiles) {
        // The canonical property name is `include`. We accept `paths` for backwards compatibility
        // with a bug in JSDoc 3.2.x.
        const staticFilePaths = conf.default.staticFiles.include ||
            conf.default.staticFiles.paths ||
            [];
        const staticFileFilter = new (require('jsdoc/src/filter').Filter)(conf.default.staticFiles);
        const staticFileScanner = new (require('jsdoc/src/scanner').Scanner)();

        staticFilePaths.forEach(filePath => {
            filePath = path.resolve(env.pwd, filePath);
            const extraStaticFiles = staticFileScanner.scan([filePath], 10, staticFileFilter);
            extraStaticFiles.forEach(fileName => {
                const sourcePath = fs.toDir(filePath);
                const toDir = fs.toDir( fileName.replace(sourcePath, outdir) );

                fs.mkPath(toDir);
                fs.copyFileSync(fileName, toDir);
            });
        });
    }

    if (sourceFilePaths.length) {
        sourceFiles = shortenPaths( sourceFiles, path.commonPrefix(sourceFilePaths) );
    }
    data().each(doclet => {
        let docletPath;
        const url = helper.createLink(doclet);

        helper.registerLink(doclet.longname, url);

        // add a shortened version of the full path
        if (doclet.meta) {
            docletPath = getPathFromDoclet(doclet);
            docletPath = sourceFiles[docletPath].shortened;
            if (docletPath) {
                doclet.meta.shortpath = docletPath;
            }
        }
    });

    data().each(doclet => {
        const url = helper.longnameToUrl[doclet.longname];

        if (url.includes('#')) {
            doclet.id = helper.longnameToUrl[doclet.longname].split(/#/).pop();
        } else {
            doclet.id = doclet.name;
        }

        if ( needsSignature(doclet) ) {
            addSignatureParams(doclet);
            addSignatureReturns(doclet);
            addAttribs(doclet);
        }
    });

    // do this after the urls have all been generated
    data().each(doclet => {
        doclet.ancestors = getAncestorLinks(doclet);

        if (doclet.kind === 'member') {
            addSignatureTypes(doclet);
            addAttribs(doclet);
        }

        if (doclet.kind === 'constant') {
            addSignatureTypes(doclet);
            addAttribs(doclet);
            doclet.kind = 'member';
        }
    });

    const members = helper.getMembers(data);
    members.tutorials = tutorials.children;

    // output pretty-printed source files by default
    const outputSourceFiles = conf.default && conf.default.outputSourceFiles !== false;

    // add template helpers
    Object.assign(view, {
        find, linkto, resolveAuthorLinks, tutoriallink, outputSourceFiles,
        htmlsafe, htmltext, htmllist, summarize, highlight, typeLabel
    });

    // once for all
    attachModuleSymbols( find({ longname: { left: 'module:' } }), members.modules );

    // generate the pretty-printed source files first so other pages can link to them
    if (outputSourceFiles) {
        generateSourceFiles(sourceFiles, opts.encoding);
    }

    if (members.globals.length) {
        generate('Global', [{ kind: 'globalobj' }], globalUrl);
    }

    // index page displays information from package.json and lists files
    const files = find({ kind: 'file' });
    const packages = find({ kind: 'package' });

    // Create topics JSON
    const topics = JSON.stringify({
        topic: 'RapidContext/JavaScript API',
        url: 'doc/js/index.html',
        children: find({ kind: ['namespace', 'class', 'interface'] }).map((ns) => {
            return {
                topic: ns.longname,
                url: `doc/js/${helper.longnameToUrl[ns.longname]}`,
                children: find({ memberof: ns.longname }).map((m) => {
                    const blocked = ['namespace', 'class', 'interface', 'typedef'];
                    const isMethod = m.kind == 'function' && !m.id.startsWith('.');
                    const isEvent = m.kind == 'event';
                    const topic = [
                        isMethod && '\u25aa',
                        isEvent && '\u25ab',
                        m.name.replaceAll(':', '.')
                    ].filter(Boolean).join('');
                    const url = `doc/js/${helper.longnameToUrl[m.longname]}`;
                    return !blocked.includes(m.kind) && { topic, url };
                }).filter(Boolean).sort((a, b) => (a.topic < b.topic) ? -1 : (a.topic > b.topic) ? 1 : 0)
            };
        })
    }, null, 2);
    fs.writeFileSync(path.join(outdir, 'topics.json'), topics, 'utf8');

    generate('JavaScript API Index', packages.concat(files), indexUrl);

    // set up the lists that we'll use to generate pages
    const classes = taffy(members.classes);
    const modules = taffy(members.modules);
    const namespaces = taffy(members.namespaces);
    const mixins = taffy(members.mixins);
    const externals = taffy(members.externals);
    const interfaces = taffy(members.interfaces);

    Object.keys(helper.longnameToUrl).forEach(longname => {
        const myClasses = helper.find(classes, { longname });
        const myExternals = helper.find(externals, { longname });
        const myInterfaces = helper.find(interfaces, { longname });
        const myMixins = helper.find(mixins, { longname });
        const myModules = helper.find(modules, { longname });
        const myNamespaces = helper.find(namespaces, { longname });

        if (myModules.length) {
            generate(`Module ${myModules[0].longname}`, myModules, helper.longnameToUrl[longname]);
        }

        if (myClasses.length) {
            generate(`Class ${myClasses[0].longname}`, myClasses, helper.longnameToUrl[longname]);
        }

        if (myNamespaces.length) {
            generate(`Namespace ${myNamespaces[0].longname}`, myNamespaces, helper.longnameToUrl[longname]);
        }

        if (myMixins.length) {
            generate(`Mixin ${myMixins[0].longname}`, myMixins, helper.longnameToUrl[longname]);
        }

        if (myExternals.length) {
            generate(`External ${myExternals[0].longname}`, myExternals, helper.longnameToUrl[longname]);
        }

        if (myInterfaces.length) {
            generate(`Interface ${myInterfaces[0].longname}`, myInterfaces, helper.longnameToUrl[longname]);
        }
    });

    // TODO: move the tutorial functions to templateHelper.js
    function generateTutorial(title, tutorial, filename) {
        const tutorialData = {
            title: title,
            header: tutorial.title,
            content: tutorial.parse(),
            children: tutorial.children
        };
        const tutorialPath = path.join(outdir, filename);
        let html = view.render('tutorial.tmpl', tutorialData);

        // yes, you can use {@link} in tutorials too!
        html = helper.resolveLinks(html); // turn {@link foo} into <a href="foodoc.html">foo</a>

        fs.writeFileSync(tutorialPath, html, 'utf8');
    }

    // tutorials can have only one parent so there is no risk for loops
    function saveChildren({ children }) {
        children.forEach(child => {
            generateTutorial(`Tutorial: ${child.title}`, child, helper.tutorialToUrl(child.name));
            saveChildren(child);
        });
    }

    saveChildren(tutorials);
};
