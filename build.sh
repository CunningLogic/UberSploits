#!/bin/sh
JAVAC=`which javac`
if [ $JAVAC ]; then
 echo Building classes ...
 $JAVAC -cp libs/commons-net-3.8.0.jar:libs/jSerialComm-2.7.0.jar:src src/com/cunninglogic/ubersploits/CRC.java
 RESULT1=$?
 $JAVAC -cp libs/commons-net-3.8.0.jar:libs/jSerialComm-2.7.0.jar:src src/com/cunninglogic/ubersploits/Main.java
 RESULT2=$?
 if [ $RESULT1 = 0 ] && [ $RESULT2 = 0 ]; then
  echo OK
 fi 
else
 echo Error: No javac. Please install javac/jdk.
fi
