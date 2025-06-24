COMMIT  := $(shell git rev-parse --short=8 HEAD)
DATE    := $(or $(DATE),$(shell date '+%F'))
VER     := $(if $(VERSION),$(patsubst v%,%,$(VERSION)),$(shell date '+%Y.%m.%d').$(COMMIT)-SNAPSHOT)
REPO    := ghcr.io/baraverkstad/rapidcontext
TAG     := $(or $(VERSION),latest)
ARCH    := linux/amd64,linux/arm64
MAKE    := $(MAKE) --no-print-directory
MAVEN   := mvn --batch-mode --no-transfer-progress

define FOREACH
    for DIR in src/plugin/*/; do \
        $(MAKE) -C $$DIR -f ../Makefile.plugin VERSION=$(VER) $(1) || exit ; \
    done
endef

all:
	@echo '🌈 Makefile commands'
	@grep -E -A 1 '^#' Makefile | awk 'BEGIN { RS = "--\n"; FS = "\n" }; { sub("#+ +", "", $$1); sub(":.*", "", $$2); printf " · make %-18s- %s\n", $$2, $$1}'
	@echo
	@echo '🚀 Release builds'
	@echo ' · make VERSION=v2022.08 clean setup build doc test package publish'


# Cleanup intermediary files
clean:
	rm -rf package-lock.json node_modules/ \
		classes/ test/classes/ lib/*.jar plugin/ target/ \
		src/plugin/system/files/js/rapidcontext.*.min.js \
		doc.zip doc/js/* \
		tmp/ rapidcontext-*.zip
	find . -name .DS_Store -delete
	find doc/java -mindepth 1 -not -name "topics.json" -delete
	$(call FOREACH,clean)


# Setup development environment
setup: clean
	npm install --omit=optional
	npm list
	$(MAVEN) -Drevision=$(VER) dependency:tree dependency:copy-dependencies -Dmdep.useSubDirectoryPerScope=true
	cp target/dependency/compile/*.jar lib/
	cp target/dependency/test/*.jar test/lib/
	cp target/dependency/runtime/mariadb-*.jar src/plugin/jdbc/lib
	cp target/dependency/runtime/postgresql-*.jar src/plugin/jdbc/lib


# Compile source and build plug-ins
build: build-java build-plugins

build-java:
	rm -rf classes/ lib/rapidcontext-*.jar
	mkdir -p classes/
	javac -d "classes" -classpath "lib/*" --release 21 \
		-sourcepath "src/java" \
		-g -deprecation \
		-Xlint:all,-path,-serial \
		-Xdoclint:all,-missing \
		$(shell find src/java -name '*.java')
	cp src/plugin/system/files/images/logotype*.png classes/org/rapidcontext/app/ui/
	mkdir -p classes/META-INF/
	echo "Package-Title: rapidcontext" >> classes/META-INF/MANIFEST.MF
	echo "Package-Version: $(VER)" >> classes/META-INF/MANIFEST.MF
	echo "Package-Date: $(DATE)" >> classes/META-INF/MANIFEST.MF
	echo "Main-Class: org.rapidcontext.app.Main" >> classes/META-INF/MANIFEST.MF
	echo "Class-Path: ." >> classes/META-INF/MANIFEST.MF
	ls lib/*.jar | xargs -L1 | sed 's|lib/|  |' >> classes/META-INF/MANIFEST.MF
	jar -c -f lib/rapidcontext-$(VER).jar -m classes/META-INF/MANIFEST.MF -C classes/ . -C src/java .

build-plugins: CLASSPATH=$(wildcard $(PWD)/lib/rapidcontext-*.jar)
build-plugins:
	rm -rf plugin/ src/plugin/system/files/js/rapidcontext.*.min.js
	mkdir -p plugin/
	npx esbuild --bundle --minify \
		--platform=browser --target=chrome76,firefox71,safari13 \
		--outfile=src/plugin/system/files/js/rapidcontext.$(VER).min.js \
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
	cd doc/ && zip -rq9 ../doc.zip .

doc-java:
	find doc/java -mindepth 1 -not -name "topics.json" -delete
	javadoc -quiet -d "doc/java" -classpath "lib/*" --release 21 \
		-sourcepath "src/java" -subpackages "org.rapidcontext" \
		-group "Application Layer" "org.rapidcontext.app:org.rapidcontext.app.*" \
		-group "Core Library Layer" "org.rapidcontext.core:org.rapidcontext.core.*" \
		-group "Utilities Layer" "org.rapidcontext.util:org.rapidcontext.util.*" \
		-version -use -windowtitle "RapidContext $(VER) Java API" \
		-stylesheetfile "share/javadoc/stylesheet.css" \
		-Xdoclint:all

doc-js:
	rm -rf doc/js/
	mkdir -p doc/js/
	npx jsdoc -c .jsdoc.json -t share/jsdoc-template-rapidcontext/ -d doc/js/ -r src/plugin/system/files/js/
	sed -i.bak -e 's/[[:space:]]*$$//' doc/js/*.html
	rm -f doc/js/*.bak


# Run tests & code style checks
test: test-css test-html test-js test-java

test-css:
	npx stylelint 'src/plugin/*/files/**/*.css' 'share/**/*.css' '!**/*.min.css'

test-html:
	npx html-validate 'doc/*.html' 'src/plugin/*/files/index.tmpl'

test-js:
	npx eslint src/plugin test/src/js --fix \
		--ignore-pattern 'src/plugin/legacy/**/*.js' \
		--ignore-pattern '**/*.min.js' \
		--ignore-pattern '**/MochiKit.js'
	node --import ./test/src/js/loader.mjs --test 'test/**/*.test.mjs'

test-java: test-java-compile
	java -classpath "lib/*:test/lib/*:test/classes:test/src/java" \
		-javaagent:test/lib/jacocoagent-0.8.11.jar=destfile=tmp/test/jacoco.exec \
		org.junit.runner.JUnitCore $(file < test/classes/test.lst)
	java -jar test/lib/jacococli-0.8.11.jar report \
		tmp/test/jacoco.exec \
		--classfiles lib/rapidcontext-*.jar \
		--xml tmp/test/jacoco.xml

test-java-compile:
	rm -rf test/classes/ tmp/test/
	mkdir -p test/classes/ tmp/test/
	javac -d "test/classes" -classpath "lib/*:test/lib/*" --release 21 \
		-sourcepath "test/src/java" \
		-g -deprecation \
		-Xlint:all,-path,-serial \
		-Xdoclint:all,-missing \
		$(shell find test/src/java -name '*.java')
	find test/classes -name "*Test*.class" | \
		sed -e 's|test/classes/||' -e 's|.class||' -e 's|/|.|g' | \
		xargs > test/classes/test.lst

test-sonar-scan:
	sonar-scanner \
		-Dsonar.organization=baraverkstad \
		-Dsonar.projectKey=baraverkstad_rapidcontext \
		-Dsonar.sources=src \
		-Dsonar.exclusions="src/java/**/package.html,src/plugin/legacy/**/*,src/plugin/system/files/js/*.min.js,src/plugin/system/files/js/MochiKit.js,src/plugin/test/**/*" \
		-Dsonar.java.source=21 \
		-Dsonar.java.binaries=classes,src/plugin/*/classes \
		-Dsonar.java.libraries=lib/*.jar \
		-Dsonar.java.test.binaries=test/classes \
		-Dsonar.java.test.libraries=test/lib/*.jar \
		-Dsonar.coverage.jacoco.xmlReportPaths=tmp/test/jacoco.xml \
		-Dsonar.host.url=https://sonarcloud.io


# Package downloads for distribution
package: package-war package-zip package-mac

package-war:
	rm -rf tmp/war/ rapidcontext-war-*.zip
	mkdir -p tmp/war/
	cp -r *.md plugin doc.zip src/web/* tmp/war/
	cp -r lib tmp/war/WEB-INF/
	rm -f tmp/war/WEB-INF/lib/{servlet-api,jetty-*,slf4j-*}.jar
	jar -cvf tmp/rapidcontext.war -C tmp/war/ .
	cd tmp/ && zip -rq9 ../rapidcontext-war-$(VER).zip rapidcontext.war

package-zip:
	rm -rf tmp/rapidcontext-*/ rapidcontext-2*.zip
	mkdir -p tmp/rapidcontext-$(VER)/
	cp -r *.md bin lib plugin share doc.zip tmp/rapidcontext-$(VER)/
	cd tmp/ && zip -rq9 ../rapidcontext-$(VER).zip rapidcontext-$(VER)

package-mac:
	rm -rf tmp/RapidContext.app/ rapidcontext-mac-*.zip
	mkdir -p tmp/RapidContext.app/
	cp -r src/mac/app/* tmp/RapidContext.app/
	cp -r *.md bin lib plugin share doc.zip tmp/RapidContext.app/Contents/Resources/
	sed -i.bak "s/@build.version@/$(VER)/" tmp/RapidContext.app/Contents/Info.plist
	rm -f tmp/RapidContext.app/Contents/Info.plist.bak
	cd tmp/ && zip -rq9 ../rapidcontext-mac-$(VER).zip RapidContext.app


# Publish to Docker and Maven
publish: publish-docker publish-maven

publish-docker: package-zip
	@echo "📦 Publishing to Docker repository..."
	rm -rf tmp/docker/
	mkdir -p tmp/docker/
	cp -r share/docker/* tmp/docker/
	cp rapidcontext-$(VER).zip tmp/docker/
	cd tmp/docker && docker buildx build . -t $(REPO):$(TAG) --build-arg VERSION=$(VER) --platform $(ARCH) --push

publish-maven:
	@echo "📦 Publishing to Maven repository..."
	$(MAVEN) deploy:deploy-file \
		-DgroupId=org.rapidcontext \
		-DartifactId=rapidcontext-api \
		-Dversion=$(VER) \
		-Dpackaging=jar \
		-Dfile=lib/rapidcontext-$(VER).jar \
		-DupdateReleaseInfo=$(if $(VERSION),true,false) \
		-DuniqueVersion=$(if $(VERSION),true,false) \
		-DrepositoryId=github \
		-Durl=https://maven.pkg.github.com/baraverkstad/rapidcontext


# Run local development server
run: build package-zip
	mkdir -p tmp/docker/
	cp -r share/docker/* tmp/docker/
	cp rapidcontext-$(VER).zip tmp/docker/
	cd tmp/docker && docker compose build --build-arg VERSION=$(VER) --pull
	cd tmp/docker && docker compose run --rm --service-ports rapidcontext


# List outdated external dependencies
list-outdated:
	@echo --== node/npm dependencies ==--
	npm outdated
	@echo
	@echo --== maven dependencies ==--
	$(MAVEN) -Drevision=$(VER) versions:display-dependency-updates


shell: build
	jshell --class-path "lib/*:$(CLASSPATH)" test/jshell/bootstrap.jshell

fix-copyright:
	git grep -PIlwz Copyright -- ':!doc/external/*' | \
		xargs -0 -n 1 sed -i.bak -E -e 's/(20[0-9]{2})-20[0-9]{2}/\1-2025/'
	find . -name "*.bak" -delete

fix-trailing-space:
	git grep -PIlz '\s+$$' -- ':!*.bat' ':!src/plugin/system/files/fonts' | \
		xargs -0 -n 1 sed -i.bak -E -e 's/[[:space:]]*$$//'
	find . -name "*.bak" -delete
