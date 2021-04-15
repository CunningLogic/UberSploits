#!/bin/sh
JAVA=`which java`
if [ $JAVA ]; then
 $JAVA -cp libs/commons-net-3.8.0.jar:libs/jSerialComm-2.7.0.jar:src com/cunninglogic/ubersploits/Main
else
 echo Error: No java. Please install java/jdk.
fi
