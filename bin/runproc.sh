#!/bin/bash
. `dirname $0`/common.sh
"$JAVA" $JAVA_OPTS -jar lib/rapidcontext-*.jar --script "$@"
