# CSID Event Lineage Demo Project

## Background

This demo is a simple use case showing a simple credit card tracing example for the proposed CSID Event Lineage project.

To learn more about the aims of the project please refer to the documentation at [Event Lineage](https://bit.ly/3CSnHAH)


## Running Demo Locally 
Prequisites:
* Docker
* Java 11 or later
* Maven

First time build:
```
./run_demo.sh
```

Once containers are up and Connect platform initialized start replicators:
```
demo/connect/submit_replicator_dc1_to_dc2.sh
demo/connect/submit_replicator_dc2_to_dc1.sh
```
if commands above return 
``
curl: (52) Empty reply from server
``
it means that Connect platform hasn't loaded yet and is not ready to accept requests and submti replicator commands have to be re-run.

Once containers are up after a delay of approximately 1-2 minutes trace/payload information 
will be available in the Jaeger UI at http://0.0.0.0:16686

In addition:
* Confluent Control Centre is available at http://0.0.0.0:9021 and http://0.0.0.0:9022

To clean up any docker container from the demo run `docker-compose down -v` from the `/demo` folder.




