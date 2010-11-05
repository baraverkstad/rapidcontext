#!/bin/bash
. `dirname $0`/common.sh
"$JAVA" $JAVA_OPTS -cp lib/rapidcontext-*.jar org.rapidcontext.app.CmdLineApplication "$@"
