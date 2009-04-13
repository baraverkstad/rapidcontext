#!/bin/bash
. `dirname $0`/common.sh
echo "Base Dir:  " `pwd`
echo "JAVA_HOME:  $JAVA_HOME"
echo "JAVA_OPTS:  $JAVA_OPTS"
echo "CLASSPATH:  $CLASSPATH"
echo
"$JAVA_HOME/bin/java" $JAVA_OPTS org.rapidcontext.app.ServerApplication "$@"
