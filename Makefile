DATE    := $(or $(DATE),$(shell date '+%F'))
VER     := $(if $(VERSION),$(patsubst v%,%,$(VERSION)),$(shell date '+%Y.%m.%d-beta'))
REPO    := 'ghcr.io/baraverkstad/rapidcontext'
TAG     := $(or $(VERSION),'latest')
ARCH    := 'linux/amd64,linux/arm64'
MAKE    := $(MAKE) --no-print-directory


all:
	@echo '🌈 Makefile commands'
	@grep -E -A 1 '^#' Makefile | awk 'BEGIN { RS = "--\n"; FS = "\n" }; { sub("#+ +", "", $$1); sub(":.*", "", $$2); printf " · make %-18s- %s\n", $$2, $$1}'
	@echo
	@echo '🚀 Release builds'
	@echo ' · make VERSION=v2022.08 clean setup build doc test package'
	@echo
	@echo '💡 Related commands'
	@echo ' · npm outdated           - Show outdated libraries and tools'


# Cleanup intermediary files
clean:
	rm -rf package-lock.json node_modules/ plugin/ tmp/ rapidcontext-*.zip
	find . -name .DS_Store -delete
	@$(MAKE) -C src/plugin/system clean
	@$(MAKE) -C src/plugin/local clean
	@$(MAKE) -C src/plugin/cmdline clean
	@$(MAKE) -C src/plugin/http clean
	@$(MAKE) -C src/plugin/jdbc clean
	@$(MAKE) -C src/plugin/legacy clean
	@$(MAKE) -C src/plugin/test clean


# Setup development environment
setup: clean
	npm install --omit=optional


# Compile source and build plug-ins
build: build-java build-plugins

build-java:
	DATE=$(DATE) VERSION=$(VER) ant compile

build-plugins: CLASSPATH=$(wildcard $(PWD)/lib/rapidcontext-*.jar)
build-plugins:
	rm -rf plugin
	mkdir -p plugin
	@$(MAKE) -C src/plugin/system VERSION=$(VER)
	@$(MAKE) -C src/plugin/local VERSION=$(VER)
	@$(MAKE) -C src/plugin/cmdline VERSION=$(VER)
	@$(MAKE) -C src/plugin/http VERSION=$(VER)
	@$(MAKE) -C src/plugin/jdbc VERSION=$(VER)
	@$(MAKE) -C src/plugin/legacy VERSION=$(VER)
	@$(MAKE) -C src/plugin/test VERSION=$(VER)
	@echo
	cp src/plugin/*/*.plugin plugin/
	for FILE in plugin/*.plugin ; do mv $$FILE $${FILE%-$(VER).plugin}.zip ; done
	mkdir -p plugin/local
	unzip -d plugin/local plugin/local.zip
	rm -f plugin/local.zip


# Generate API documentation
doc: doc-java doc-js
	rm -f doc.zip
	cd doc/ && zip -r9 ../doc.zip .

doc-java:
	find doc/java -mindepth 1 -not -name "topics.json" -delete
	javadoc -quiet -d "doc/java" -classpath "lib/*" --release 11 \
		-sourcepath "src/java" -subpackages "org.rapidcontext" \
		-group "Application Layer" "org.rapidcontext.app:org.rapidcontext.app.*" \
		-group "Core Library Layer" "org.rapidcontext.core:org.rapidcontext.core.*" \
		-group "Utilities Layer" "org.rapidcontext.util:org.rapidcontext.util.*" \
		-version -use -windowtitle "RapidContext $(VER) Java API" \
		-stylesheetfile "share/javadoc/stylesheet.css" \
		-Xdoclint:all

doc-js:
	mkdir -p doc/js/
	rm -rf doc/js/*
	npx jsdoc -c .jsdoc.json -t share/jsdoc-template-rapidcontext/ -d doc/js/ -r src/plugin/system/files/js/
	sed -i.bak -e 's/[[:space:]]*$$//' doc/js/*.html
	rm -f doc/js/*.bak


# Run tests & code style checks
test: test-css test-html test-js test-java

test-css:
	npx stylelint 'src/plugin/system/files/**/*.css' 'share/**/*.css' '!**/*.min.css'

test-html:
	npx html-validate 'doc/*.html' 'src/plugin/*/files/index.tmpl'

test-js:
	npx eslint 'src/plugin/**/*.{js,cjs,mjs}' \
		--ignore-pattern 'src/plugin/legacy/**/*.js' \
		--ignore-pattern '**/*.min.js' \
		--ignore-pattern '**/MochiKit.js'

test-java:
	mkdir -p test/classes/
	rm -rf test/classes/*
	javac -d "test/classes" -classpath "lib/*:test/lib/*" --release 11 \
		-sourcepath "test/src/java" \
		-g -deprecation \
		-Xlint:all,-missing-explicit-ctor,-serial \
		-Xdoclint:all,-missing \
		$(shell find test/src/java -name '*.java')
	find test/classes -name "*Test*.class" | \
		sed -e 's|test/classes/||' -e 's|.class||' -e 's|/|.|g' \
		> test/classes/test.lst
	java -classpath "lib/*:test/lib/*:test/classes:test/src/java" \
		org.junit.runner.JUnitCore $(shell cat test/classes/test.lst)


# Package downloads for distribution
package: package-war package-zip package-mac

package-war:
	mkdir -p tmp/war/
	cp -r *.md plugin doc.zip src/web/* tmp/war/
	cp -r lib tmp/war/WEB-INF/
	rm -f tmp/war/WEB-INF/lib/{servlet-api,jetty-*,slf4j-*}.jar
	jar -cvf tmp/rapidcontext.war -C tmp/war/ .
	cd tmp/ && zip -r9 ../rapidcontext-$(VER)-war.zip rapidcontext.war

package-zip:
	mkdir -p tmp/rapidcontext-$(VER)/
	cp -r *.md bin lib plugin share doc.zip tmp/rapidcontext-$(VER)/
	cd tmp/ && zip -r9 ../rapidcontext-$(VER).zip rapidcontext-$(VER)

package-mac:
	mkdir -p tmp/RapidContext.app/
	cp -r src/mac/app/* tmp/RapidContext.app/
	cp -r *.md bin lib plugin share doc.zip tmp/RapidContext.app/Contents/Resources/
	sed -i.bak "s/@build.version@/$(VER)/" tmp/RapidContext.app/Contents/Info.plist
	rm -f tmp/RapidContext.app/Contents/Info.plist.bak
	cd tmp/ && zip -r9 ../rapidcontext-$(VER)-mac.zip RapidContext.app

package-docker: package-zip
	mkdir -p tmp/docker/
	cp -r share/docker/* tmp/docker/
	cp rapidcontext-$(VER).zip tmp/docker/
	cd tmp/docker && docker buildx build . -t $(REPO):$(TAG) --build-arg VERSION=$(VER) --platform $(ARCH) --push


# Run local development server
run: build package-zip
	mkdir -p tmp/docker/
	cp -r share/docker/* tmp/docker/
	cp rapidcontext-$(VER).zip tmp/docker/
	cd tmp/docker && docker compose build --build-arg VERSION=$(VER) --pull
	cd tmp/docker && docker compose run --rm --service-ports rapidcontext


# Update source code copyright year
fix-copyright:
	git grep -Ilrwz Copyright -- ':!doc/external/*' | \
		xargs -0 -n 1 sed -i.bak -E 's/(20[0-9]{2})-20[0-9]{2}/\1-2023/'
	find . -name "*.bak" -delete
