mvn clean package

echo "Fetching opentelemetry library"

curl https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/1.13.0/opentelemetry-javaagent-1.13.0.jar -o opentelemetry-javaagent-1.13.0.jar
cp opentelemetry-javaagent-1.13.0.jar demo/javaapps/opentelemetry-javaagent-1.13.0.jar
cp opentelemetry-javaagent-1.13.0.jar demo/connect/opentelemetry-javaagent-1.13.0.jar
cp lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jarfile demo/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar
cp lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jarfile demo/connect/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar

echo "Stepping into the directory to gather all jars"
cd demo/javaapps
./gatherjars.sh

cd ..
echo "Building the demo environment"
docker-compose build javaapps
docker-compose down -v
docker-compose up -d
cd ..