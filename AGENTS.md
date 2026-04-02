# Project: RapidContext
*Platform for building rich dynamic web apps — Java server, JS client, plugin architecture.*

## References
- [README.md] — installation, Docker, Maven usage
- [doc/index.html] — platform documentation index
- [Makefile] — run `make` to list available targets
- [pom.xml] — Java dependency manifest
- [package.json] — JS tooling dependencies
- [.github/workflows/] — GitHub Actions; publishes to `ghcr.io/baraverkstad/rapidcontext`

## Goals & Ethos
- Radical brevity
- Safe-by-design
- Stable APIs
- Minimal dependencies

## Code Style
- **Line length:** soft 80, hard 120
- **Comments:** explain *why*, not *what*; no code flow narration
- **API docs:** Javadoc/JSDoc in public APIs, never in test code
- **Variables:** short but descriptive; single-char for loops, etc
- **Code flow:** minimal ceremony, no factory patterns
- **Java:** Java 21; prefer standard Java APIs over Apache Commons
- **JS:**  ongoing migration to ESM modules (`.mjs`)

## Constraints
- **File I/O:** use `org.rapidcontext.util` classes for file operations
- **Escaping:** use `TextEncoding.encodeXml`, not `StringEscapeUtils`

## Design Notes
- **Java layers:** `org.rapidcontext.app` / `org.rapidcontext.core` / `org.rapidcontext.util`
- **Auth:** JWT login tokens signed with HMAC-SHA256; password changes invalidate tokens
- **App manifests:** `procedures` key auto-mapped to `this.proc.*` via camelCase transform (`my-app/proc-name` → `this.proc.myApp.procName`)
- **Logging:** `java.util.logging` with custom extensions in `org.rapidcontext.util.logging`; default config in `lib/logging.properties`
- **Documentation:** `doc/*.html` files; also viewable via platform Help App
- **JS engine:** Rhino; use `LambdaFunction` (not deprecated `NativeJavaMethod`) to expose Java methods to JavaScript

## Build & Test
```
make                                        # show top-level build targets (read Makefile for all)
make clean setup doc build test             # full build (use on dependency changes)
make build test                             # build and test (use on code changes)
make test-java-unit                         # unit tests only (fastest iteration)
```
