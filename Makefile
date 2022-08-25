#
# Helpful information
#
all:
	@echo '🌈 Makefile targets'
	@echo ' · make clean      — Cleanup intermediary files'
	@echo ' · make setup      — Setup development environment'
	@echo ' · make build      — Build release artefacts'
	@echo ' · make test       — Tests & code style checks'
	@echo
	@echo '🚀 Release builds'
	@echo ' · VERSION=2022.08 make build'
	@echo
	@echo '📍 Apache Ant (and Java) must be installed separately.'


#
# Cleanup intermediary files
#
clean:
	rm -rf package-lock.json node_modules/


#
# Setup development environment
#
setup: clean
	npm install --no-optional


#
# Build release artefacts
#
build:
	ant package


#
# Tests & code style checks
#
test: test-css test-html test-js
	ant test

test-css:
	npx stylelint 'src/plugin/system/files/css/*.css' 'tools/javadoc/stylesheet.css' '!**/*.min.css'

test-html:
	npx html-validate 'doc/*.html' 'src/plugin/*/files/index.tmpl'

test-js:
	npx eslint 'src/plugin/**/*.js' \
		--ignore-pattern 'src/plugin/legacy/**/*.js' \
		--ignore-pattern '**/*.min.js' \
		--ignore-pattern '**/MochiKit.js' \
		--ignore-pattern '**/PlotKit.js'
