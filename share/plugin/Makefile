# Compiles and packages a plug-in for RapidContext. Copy this
# file to your plugin project and modify as needed.
#
# Requirements:
# - GNU Make 4.x+
# - Java SDK (javac, jar) 17+
# - Linux/BSD/MacOS command-line tools

CONFIG    := $(firstword $(wildcard plugin.* plugin/*))
DATE      := $(or $(DATE),$(shell date '+%F'))
BUILD     := $(or $(file <.build),0)
ID        := $(lastword $(shell grep -rE "^id\s[:=]" $(CONFIG)))
ifneq (,$(VERSION))
VER       := $(VERSION)
else
VER       := $(lastword $(shell grep -rE "^version\s[:=]" $(CONFIG)))
VER       := $(subst version,,$(subst :,,$(subst =,,$(VER))))
VER       := $(or $(VER),$(DATE)).$(BUILD)
endif
SRC       := $(shell test -d src && find src -name '*.java')
ifneq (,$(wildcard ../../../lib/*.jar))
CLASSPATH := ../../../lib/*:$(CLASSPATH)
endif
ifneq (,$(wildcard ../../lib/*.jar))
CLASSPATH := ../../lib/*:$(CLASSPATH)
endif
ifneq (,$(wildcard ../lib/*.jar))
CLASSPATH := ../lib/*:$(CLASSPATH)
endif
ifneq (,$(filter-out $(wildcard lib/$(ID)-*.jar),$(wildcard lib/*)))
CLASSPATH := lib/*:$(CLASSPATH)
endif

all: info clean compile package

info:
	@echo
	@echo "---~~~==#  Building Plug-in $(ID) v$(VER) ($(DATE))  #==~~~---"

clean:
	rm -rf classes/ tmp/ lib/$(ID)-*.jar $(ID)-*.{plugin,zip}

compile: $(if $(SRC),compile-java,)

compile-java:
	mkdir -p classes lib
	javac -d "classes" -classpath "$(CLASSPATH)" --release 17 \
		-g -deprecation \
		-Xlint:all,-path,-serial \
		-Xdoclint:all,-missing \
		$(SRC)
	jar -cf lib/$(ID)-$(VER).jar -C classes/ .

package:
	mkdir -p tmp
	cp -r $(filter-out Makefile classes src tmp,$(shell ls)) tmp/
	rm -f tmp/lib/rapidcontext-*.jar
	sed -i.bak \
		-e 's/^date\( *[:=]\).*$$/date\1 $(DATE)/' \
		-e 's/^version\( *[:=]\).*$$/version\1 $(VER)/' \
		tmp/$(CONFIG)
	rm -f tmp/$(CONFIG).bak
ifneq (,$(wildcard platform.*))
	sed -i.bak \
		-e 's/^date\( *[:=]\).*$$/date\1 $(DATE)/' \
		-e 's/^version\( *[:=]\).*$$/version\1 $(VER)/' \
		tmp/platform.*
	rm -f tmp/platform.*.bak
endif
	cd tmp/ && zip -rq9 ../$(ID)-$(VER).plugin .
	rm -rf tmp/
	@echo $$(($(BUILD)+1)) >.build
