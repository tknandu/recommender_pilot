#!/bin/sh

libs='bin'
for i in `ls -1 lib/external_jars/*.jar`
do
  libs="$libs:$i"
done

java -cp $libs org.recommender101.Recommender101