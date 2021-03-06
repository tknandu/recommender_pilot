#!/bin/sh
libs=''
for i in `ls -1 lib/external_jars/*.jar`
do
  if [ "$libs" == "" ]
  then
      libs="$i"
  else      
      libs="$libs:$i"
  fi
done
rm -rf bin
mkdir bin
javac -cp $libs -d bin -sourcepath src src/org/recommender101/recommender/extensions/funksvd/FunkSVDRecommender.java
javac -cp $libs -d bin -sourcepath src src/org/recommender101/eval/metrics/Precision.java
javac -cp $libs -d bin -sourcepath src src/org/recommender101/Recommender101.java
#javac -cp $libs -d bin -sourcepath src src/org/recommender101/JRItest.java
