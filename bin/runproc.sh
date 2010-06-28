#!/bin/bash
. `dirname $0`/common.sh
"$JAVA" $JAVA_OPTS org.rapidcontext.app.CmdLineApplication "$@"
