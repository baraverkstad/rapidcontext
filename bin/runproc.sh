#!/bin/bash
. `dirname $0`/common.sh
"$JAVA_HOME/bin/java" $JAVA_OPTS org.rapidcontext.app.CmdLineApplication "$@"
