# AGENTS.md
*Platform for building rich dynamic web apps — Java server, JS client, plugin architecture.*

## References
- [README.md] — installation, Docker, Maven usage
- [Makefile] — run `make` to list available targets
- [doc/] — full platform documentation
- [pom.xml] — Java dependency manifest
- [package.json] — JS tooling dependencies
- [.github/workflows/] — GitHub Actions; publishes to `ghcr.io/baraverkstad/rapidcontext`

## Goals & Ethos
- Safe, stable, minimal-dependency platform for web app development

## Code Style
- Radically brief: short variable names, minimal ceremony
- Java: descriptive names, Javadoc on all public APIs, modern Java (21)
- Java: Prefer standard Java APIs (`java.nio`, `String.isBlank()`, Servlet API) over Apache Commons
- JS: descriptive names, JSDoc on public API, ongoing migration to ESM modules (`.mjs`)

## Constraints
- Use `org.rapidcontext.util` classes for file operations, etc.
- No `StringEscapeUtils` — use `TextEncoding.encodeXml` for HTML escaping

## Design Notes
- Java layers: `org.rapidcontext.app` / `org.rapidcontext.core` / `org.rapidcontext.util`
- Plugins: `src/plugin/*/`; packaged via `share/plugin/Makefile.plugin`; uses `cp -L` so `.plugin` zips are self-contained (symlinks dereferenced)
- Auth: JWT login tokens signed with HMAC-SHA256; password changes invalidate tokens
- App manifests: `procedures` key auto-mapped to `this.proc.*` via camelCase transform (`my-app/proc-name` → `this.proc.myApp.procName`)
- JS bundling: performed by `make setup-npm` and `make build-plugins`
- Logging: `java.util.logging` with custom extensions in `org.rapidcontext.util.logging`; default config in `lib/logging.properties`
- Documentation: `doc/*.html` files; also viewable via platform Help App
- Tests: unit + integration tests; run with `make test`
