mvn clean package
echo "Stepping into the directory to gather all jars"
cd demo/javaapps
./gatherjars.sh
curl https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/1.13.0/opentelemetry-javaagent-1.13.0.jar -o opentelemetry-javaagent-1.13.0.jar
# download extension
cd ..
echo "Building the demo environment"
docker-compose build javaapps
docker-compose down
docker-compose up -d
