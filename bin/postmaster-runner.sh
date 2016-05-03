#/bin/bash!
mvn clean package
java -jar target/postmaster-runner-1.0.0-SNAPSHOT-jar-with-dependencies.jar co.poynt.postmaster.PostmasterCollectionRunner $1 $2 $3 $4 $5 $6 $7
