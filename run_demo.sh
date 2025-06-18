#!/bin/bash
set -e

if [[ $(javac -version 2>&1) != *"11."* ]]; then
  echo "❌ ERROR: You must use JDK 11 to build this project."
  exit 1
fi

mvn clean install -Dmaven.compiler.release=11

echo "Stepping into the directory to gather all jars"
cd demo/javaapps
./gatherjars.sh
curl https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/1.13.0/opentelemetry-javaagent-1.13.0.jar -o opentelemetry-javaagent-1.13.0.jar
cp ../../lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jarfile lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar
cd ..
echo "Building the demo environment"
docker-compose build javaapps
docker-compose down
docker-compose up -d
