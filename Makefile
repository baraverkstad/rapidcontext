DATE    := $(or $(DATE),$(shell date '+%F'))
VER     := $(if $(VERSION),$(patsubst v%,%,$(VERSION)),$(shell date '+%Y.%m.%d-beta'))
REPO    := 'ghcr.io/baraverkstad/rapidcontext'
TAG     := $(or $(VERSION),'latest')
ARCH    := 'linux/amd64,linux/arm64'
MAKE    := $(MAKE) --no-print-directory

define FOREACH
    for DIR in src/plugin/*/; do \
        $(MAKE) -C $$DIR -f ../Makefile.plugin VERSION=$(VER) $(1); \
    done
endef

all:
	@echo '🌈 Makefile commands'
	@grep -E -A 1 '^#' Makefile | awk 'BEGIN { RS = "--\n"; FS = "\n" }; { sub("#+ +", "", $$1); sub(":.*", "", $$2); printf " · make %-18s- %s\n", $$2, $$1}'
	@echo
	@echo '🚀 Release builds'
	@echo ' · make VERSION=v2022.08 clean setup build doc test package'
	@echo
	@echo '💡 Related commands'
	@echo ' · npm outdated           - Show outdated JS libraries and tools'
	@echo ' · mvn versions:display-dependency-updates'
	@echo '                          - Show outdated Java libraries'


# Cleanup intermediary files
clean:
	rm -rf package-lock.json node_modules/ \
		classes/ lib/*.jar plugin/ target/ \
		doc.zip doc/js/* \
		tmp/ rapidcontext-*.zip
	find . -name .DS_Store -delete
	find doc/java -mindepth 1 -not -name "topics.json" -delete
	$(call FOREACH,clean)


# Setup development environment
setup: clean
	npm install --omit=optional
	mvn dependency:copy-dependencies -Dmdep.useSubDirectoryPerScope=true
	cp target/dependency/compile/*.jar lib/
	cp target/dependency/test/*.jar test/lib/


# Compile source and build plug-ins
build: build-java build-plugins

build-java:
	mkdir -p classes/
	rm -rf classes/* lib/rapidcontext-*.jar
	javac -d "classes" -classpath "lib/*" --release 17 \
		-sourcepath "src/java" \
		-g -deprecation \
		-Xlint:all,-path,-serial \
		-Xdoclint:all,-missing \
		$(shell find src/java -name '*.java')
	cp src/plugin/system/files/images/logotype*.png classes/org/rapidcontext/app/ui/
	echo "build.version = $(VER)" >> classes/org/rapidcontext/app/build.properties
	echo "build.date = $(DATE)" >> classes/org/rapidcontext/app/build.properties
	mkdir -p classes/META-INF/
	echo "Package-Title: rapidcontext" >> classes/META-INF/MANIFEST.MF
	echo "Package-Version: $(VER)" >> classes/META-INF/MANIFEST.MF
	echo "Package-Date: $(DATE)" >> classes/META-INF/MANIFEST.MF
	echo "Main-Class: org.rapidcontext.app.Main" >> classes/META-INF/MANIFEST.MF
	echo "Class-Path: ." >> classes/META-INF/MANIFEST.MF
	ls lib/*.jar | xargs -L1 | sed 's|lib/|  |' >> classes/META-INF/MANIFEST.MF
	jar -c -m classes/META-INF/MANIFEST.MF -f lib/rapidcontext-$(VER).jar -C classes/ .

build-plugins: CLASSPATH=$(wildcard $(PWD)/lib/rapidcontext-*.jar)
build-plugins:
	mkdir -p plugin/
	rm -rf plugin/*
	npx esbuild --bundle --minify \
		--platform=browser --target=chrome69,firefox65,safari11 \
		--outfile=src/plugin/system/files/js/rapidcontext.min.js \
		src/plugin/system/files/js/rapidcontext/index.mjs
	$(call FOREACH,all)
	@echo
	cp src/plugin/*/*.plugin plugin/
	for FILE in plugin/*.plugin ; do mv $$FILE $${FILE%-$(VER).plugin}.zip ; done
	mkdir -p plugin/local/
	unzip -d plugin/local/ plugin/local.zip
	rm -f plugin/local.zip


# Generate API documentation
doc: doc-java doc-js
	rm -f doc.zip
	cd doc/ && zip -r9 ../doc.zip .

doc-java:
	find doc/java -mindepth 1 -not -name "topics.json" -delete
	javadoc -quiet -d "doc/java" -classpath "lib/*" --release 17 \
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
	npx eslint src/plugin test/src/js --ext '.js,.cjs,.mjs,.html,.tmpl' \
		--ignore-pattern 'src/plugin/legacy/**/*.js' \
		--ignore-pattern '**/*.min.js' \
		--ignore-pattern '**/MochiKit.js'
	node --import ./test/src/js/loader.mjs --test test/src/js/

test-java:
	mkdir -p test/classes/ tmp/test/
	rm -rf test/classes/*
	javac -d "test/classes" -classpath "lib/*:test/lib/*" --release 17 \
		-sourcepath "test/src/java" \
		-g -deprecation \
		-Xlint:all,-path,-serial \
		-Xdoclint:all,-missing \
		$(shell find test/src/java -name '*.java')
	find test/classes -name "*Test*.class" | \
		sed -e 's|test/classes/||' -e 's|.class||' -e 's|/|.|g' \
		> test/classes/test.lst
	java -classpath "lib/*:test/lib/*:test/classes:test/src/java" \
		-javaagent:test/lib/jacocoagent-0.8.11.jar=destfile=tmp/test/jacoco.exec \
		org.junit.runner.JUnitCore $(shell cat test/classes/test.lst)
	java -jar test/lib/jacococli-0.8.11.jar report \
		tmp/test/jacoco.exec \
		--classfiles lib/rapidcontext-*.jar \
		--xml tmp/test/jacoco.xml

test-sonar-scan:
	sonar-scanner \
		-Dsonar.organization=baraverkstad \
		-Dsonar.projectKey=baraverkstad_rapidcontext \
		-Dsonar.sources=src \
		-Dsonar.exclusions="src/java/**/package.html,src/plugin/legacy/**/*,src/plugin/system/files/js/*.min.js,src/plugin/system/files/js/MochiKit.js,src/plugin/test/**/*" \
		-Dsonar.java.source=17 \
		-Dsonar.java.binaries=classes,src/plugin/*/classes \
		-Dsonar.java.libraries=lib/*.jar \
		-Dsonar.java.test.binaries=test/classes \
		-Dsonar.java.test.libraries=test/lib/*.jar \
		-Dsonar.coverage.jacoco.xmlReportPaths=tmp/test/jacoco.xml \
		-Dsonar.host.url=https://sonarcloud.io


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


fix-copyright:
	git grep -PIlwz Copyright -- ':!doc/external/*' | \
		xargs -0 -n 1 sed -i.bak -E -e 's/(20[0-9]{2})-20[0-9]{2}/\1-2024/'
	find . -name "*.bak" -delete

fix-trailing-space:
	git grep -PIlz '\s+$$' -- ':!*.bat' ':!src/plugin/system/files/fonts' | \
		xargs -0 -n 1 sed -i.bak -E -e 's/[[:space:]]*$$//'
	find . -name "*.bak" -delete
