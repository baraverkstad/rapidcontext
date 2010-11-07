#!/bin/bash
. `dirname $0`/common.sh
echo "Base Dir:  " `pwd`
echo "JAVA:       $JAVA"
echo "JAVA_OPTS:  $JAVA_OPTS"
echo
mkdir -p var
echo $$ > var/server.pid
exec "$JAVA" $JAVA_OPTS -jar lib/rapidcontext-*.jar --server "$@"
