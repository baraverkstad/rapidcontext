#!/bin/bash

# Locate application directory
if [[ "$RAPIDCONTEXT_HOME" == "" ]] ; then
    RAPIDCONTEXT_HOME=`dirname $0`
    if [[ "$RAPIDCONTEXT_HOME" == "." ]] ; then
        RAPIDCONTEXT_HOME=".."
    else
        RAPIDCONTEXT_HOME=`dirname $RAPIDCONTEXT_HOME`
    fi
fi
if [[ "$RAPIDCONTEXT_HOME" == "" ]] ; then
    echo "ERROR: Failed to find base application directory." >&2
    exit 1
fi
export RAPIDCONTEXT_HOME
cd $RAPIDCONTEXT_HOME

# Outputs the Java version for the specified directory
function java_version {
    DIR=$1
    "$DIR/bin/java" -version 2> tmp.ver 1> /dev/null
    VERSION=`cat tmp.ver | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }'`
    rm tmp.ver
    echo $VERSION
}

# Verifies that a directory can serve as JAVA_HOME
function is_java_dir {
    DIR=$1
    if [[ ! -x "$DIR/bin/java" ]] ; then
        return 1
    fi
    if [[ (! -d "$DIR/jre") && (! -d "$DIR/lib") ]] ; then
        return 2
    fi
    VERSION=`java_version "$DIR" | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
    if [[ "$VERSION" == "" ]] ; then
        return 3
    elif [[ "$VERSION" -le "104" ]] ; then
        return 4
    fi
    return 0
}

# Setup JAVA_HOME variable
if is_java_dir $JAVA_HOME ; then
    :
else
    JAVA_HOME=
    for JAVA_EXE in `locate bin/java | grep java$ | xargs echo` ; do
        JAVA_HOME=`dirname "$JAVA_EXE"`
        JAVA_HOME=`dirname "$JAVA_HOME"`
        if is_java_dir $JAVA_HOME ; then
            break
        else
            JAVA_HOME=
        fi
    done
fi
if [[ "$JAVA_HOME" == "" ]] ; then
    echo "ERROR: Failed to find java version 1.4 or higher." >&2
    exit 1
fi
export JAVA_HOME

# Setup JAVA_OPTS variable
JAVA_OPTS="-Xbootclasspath/p:lib/js.jar $JAVA_OPTS"
case "$JAVA_OPTS" in
*-DDEBUG*)
    JAVA_OPTS="$JAVA_OPTS -Djava.util.logging.config.file=lib/debug.properties"
    ;;
esac
export JAVA_OPTS

# Setup CLASSPATH variable
export CLASSPATH=`find "lib" -name *.jar | xargs | sed -e "s/ /:/g"`
