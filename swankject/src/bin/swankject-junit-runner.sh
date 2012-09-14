#!/bin/bash -e

###############################################################################
#
# An alternative to running mvn test -Dtest=MyTest
# This is because I don't know how to make a javaagent and maven work
# toghether
#
###############################################################################

ME=$(basename $0)

echo
echo
echo "+-----------------------------------------------------------------------------+"
echo "| $ME: Running ..."
echo "+-----------------------------------------------------------------------------+"
echo
echo

CONF=~/.swankject.conf

if [ -f "$CONF" ] ;
then
    . "$CONF"
else
    echo "[INFO] No $CONF found ..."
fi

WAS_INTERACTIVE=''

if [ -z "$MVN_MODULE_DIR" ] ;
then
    read -p "Enter the path to the Maven module: " MVN_MODULE_DIR
    WAS_INTERACTIVE='true'
fi

if [ -z "$SWANKJECT_HOME" ] ;
then
    read -p "Enter the home directory of swankject (it look like path/to/hmdb-reloaded/swankject): " SWANKJECT_HOME
    WAS_INTERACTIVE='true'
fi

if [ -z "$TEST_TO_RUN" ] ;
then
    read -p "Enter the fully qualified class name of the test to run: " TEST_TO_RUN
    WAS_INTERACTIVE='true'
fi

if [ -z "$WAS_INTERACTIVE" ] ;
then
    echo
    echo "Non interactive mode detected, using $CONF"
    echo
else
    echo "Save config to $CONF for future runs?"
    select yn in "Yes" "No"; do
        case $yn in
            Yes ) echo -e "MVN_MODULE_DIR=$MVN_MODULE_DIR\nSWANKJECT_HOME=$SWANKJECT_HOME\nTEST_TO_RUN=$TEST_TO_RUN" >> "$CONF"; break;;
            No ) exit;;
        esac
    done
fi

echo
echo "About to run with values:"
echo "    MVN_MODULE_DIR=$MVN_MODULE_DIR"
echo "    SWANKJECT_HOME=$SWANKJECT_HOME"
echo "    TEST_TO_RUN   =$TEST_TO_RUN"
echo

cd "$MVN_MODULE_DIR"

echo
echo
echo "    +-------------------------------------------------------------------------+"
echo "    | $ME: Running Maven copy-dependencies ..."
echo "    +-------------------------------------------------------------------------+"
echo
echo
mvn dependency:copy-dependencies
echo
echo
echo "    +-------------------------------------------------------------------------+"
echo "    | $ME: Running Maven copy-dependencies DONE"
echo "    +-------------------------------------------------------------------------+"
echo
echo
echo

echo
echo
echo "    +-------------------------------------------------------------------------+"
echo "    | $ME: Building classpath ..."
echo "    +-------------------------------------------------------------------------+"
echo
echo
CP=''
for I in $(ls target/dependency/*.jar | grep -v 'aspectj')
do
    if [ ! -z $CP ] ;
    then
        CP="$CP:"
    fi
    CP="$CP$I"
done

CP="$CP:$SWANKJECT_HOME/java"
CP="$CP:$SWANKJECT_HOME/java/swankject-agent.jar"
CP="$CP:$SWANKJECT_HOME/target/swankject-0.1.0-SNAPSHOT-standalone.jar"
CP="$CP:$ASPECTJ_HOME/lib/aspectjrt.jar"
CP="$CP:target/test-classes"
echo
echo
echo "    +-------------------------------------------------------------------------+"
echo "    | $ME: Building classpath DONE"
echo "    +-------------------------------------------------------------------------+"
echo
echo

echo
echo
echo "    +-------------------------------------------------------------------------+"
echo "    | $ME: Running test with javaagent ..."
echo "    +-------------------------------------------------------------------------+"
echo
echo
set -x
java -javaagent:"$ASPECTJ_HOME/lib/aspectjweaver.jar" -classpath "$CP" org.junit.runner.JUnitCore "$TEST_TO_RUN"
set +x
echo
echo
echo "    +-------------------------------------------------------------------------+"
echo "    | $ME: Running test with javaagent DONE"
echo "    +-------------------------------------------------------------------------+"
echo
echo

echo
echo
echo "+-----------------------------------------------------------------------------+"
echo "| $ME: DONE"
echo "+-----------------------------------------------------------------------------+"
echo
echo
