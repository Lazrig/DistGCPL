#! /bin/sh

export JAVA_CP=config:build/classes:shared/lib/*:dist/lib/*
export JVM_OPTS="-Xmx256m -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties"
//java -cp $JAVA_CP $JVM_OPTS org.jppf.example.concurrentjobs.ConcurrentJobs "$1"

#java -cp $JAVA_CP $JVM_OPTS -jar dist/PatientLinkageGC-Dist-NoElm.jar  -config configs/cfg_eva_4BFs_Blk_1k.txt -data BFData/Blkd_src_b_4BFs_1k.csv

java -cp $JAVA_CP $JVM_OPTS patientlinkage.Util.Main -config configs/cfg_eva_4BFs_Blk_1k.txt -data BFData/Blkd_src_b_4BFs_1k.csv