DATE    := $(or $(DATE),$(shell date '+%F'))
TAG     := $(or $(VERSION),'latest')
VER     := $(if $(VERSION),$(patsubst v%,%,$(VERSION)),$(shell date '+%Y.%m.%d-beta'))

all:
	@echo '🌈 Makefile commands'
	@grep -E -A 1 '^#' Makefile | awk 'BEGIN { RS = "--\n"; FS = "\n" }; { sub("#+ +", "", $$1); sub(":.*", "", $$2); printf " · make %-18s- %s\n", $$2, $$1}'
	@echo
	@echo '🚀 Release builds'
	@echo ' · make VERSION=v2022.08 build build-docker'
	@echo
	@echo '📍 Apache Ant (and Java) must be installed separately.'


# Cleanup intermediary files
clean:
	rm -rf package-lock.json node_modules/


# Setup development environment
setup: clean
	npm install --omit=optional


# Build release artefacts
build:
	rm -f share/docker/rapidcontext-*.zip
	DATE=$(DATE) VERSION=$(VER) ant package

build-docker:
	cp rapidcontext-$(VER).zip share/docker/
	( \
		cd share/docker && \
		docker buildx build . \
			-t ghcr.io/baraverkstad/rapidcontext:$(TAG) \
			--build-arg VERSION=$(VER) \
			--platform linux/amd64,linux/arm64 \
			--push \
	)
	rm share/docker/rapidcontext-$(VER).zip


# Tests & code style checks
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
		--ignore-pattern '**/MochiKit.js'
