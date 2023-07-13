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
	@echo '💡 Related commands'
	@echo ' · npm outdated           - Show outdated libraries and tools'


# Cleanup intermediary files
clean:
	rm -rf package-lock.json node_modules/ plugin/ tmp/ rapidcontext-*.zip
	find . -name .DS_Store -delete


# Setup development environment
setup: clean
	npm install --omit=optional


# Compile source and build plug-ins
build:
	rm -f share/docker/rapidcontext-*.zip
	DATE=$(DATE) VERSION=$(VER) ant compile doc

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


# Run tests & code style checks
test: test-css test-html test-js
	ant test

test-css:
	npx stylelint 'src/plugin/system/files/**/*.css' 'tools/javadoc/stylesheet.css' '!**/*.min.css'

test-html:
	npx html-validate 'doc/*.html' 'src/plugin/*/files/index.tmpl'

test-js:
	npx eslint 'src/plugin/**/*.{js,cjs,mjs}' \
		--ignore-pattern 'src/plugin/legacy/**/*.js' \
		--ignore-pattern '**/*.min.js' \
		--ignore-pattern '**/MochiKit.js'


# Package downloads for distribution
package: package-war package-zip package-mac

package-war:
	mkdir -p tmp/war/
	cp -r *.md plugin doc.zip src/web/* tmp/war/
	cp -r lib tmp/war/WEB-INF/
	rm tmp/war/WEB-INF/lib/{servlet-api,jetty-*,slf4j-*}.jar
	jar -cvf tmp/rapidcontext.war -C tmp/war/ .
	cd tmp/ && zip -r9 ../rapidcontext-$(VER)-war.zip rapidcontext.war

package-zip:
	mkdir -p tmp/rapidcontext-$(VER)/
	cp -r *.md bin lib plugin share doc.zip tmp/rapidcontext-$(VER)/
	cd tmp/ && zip -r9 ../rapidcontext-$(VER).zip rapidcontext-$(VER)

package-mac:
	mkdir -p tmp/RapidContext.app
	cp -r src/mac/app/* tmp/RapidContext.app/
	cp -r *.md bin lib plugin share doc.zip tmp/RapidContext.app/Contents/Resources/
	sed -Ei '' "s/@build.version@/$(VER)/" tmp/RapidContext.app/Contents/Info.plist
	cd tmp/ && zip -r9 ../rapidcontext-$(VER)-mac.zip RapidContext.app


# Run local development server
run:
	cp rapidcontext-$(VER).zip share/docker/
	cd share/docker && docker compose build --build-arg VERSION=$(VER) --pull
	rm share/docker/rapidcontext-$(VER).zip
	cd share/docker && docker compose run --rm --service-ports rapidcontext


# Update source code copyright year
fix-copyright:
	git grep -Ilrwz Copyright -- ':!doc/external/*' | \
		xargs -0 -n 1 sed -Ei '' 's/(20[0-9]{2})-20[0-9]{2}/\1-2023/'
