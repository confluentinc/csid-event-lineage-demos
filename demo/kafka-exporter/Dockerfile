# syntax = docker/dockerfile:1.4
FROM golang:stretch

# RUN GO111MODULE=on go install go.opentelemetry.io/collector/cmd/builder@v0.47.0

COPY ./ /go/kafkaexporter

COPY <<-"EOF" /go/run.sh
#!/bin/bash
go run ./collector $@
EOF

RUN chmod 755 /go/run.sh

COPY <<-"EOF" /go/otelcol-builder.yaml
dist:
  otelcol_version: "0.47.0"
exporters:
  - import: go.opentelemetry.io/collector/exporter/loggingexporter
    gomod: go.opentelemetry.io/collector v0.47.0
  - gomod: "github.com/open-telemetry/opentelemetry-collector-contrib/exporter/prometheusexporter v0.47.0"
  - gomod: "github.com/open-telemetry/opentelemetry-collector-contrib/exporter/zipkinexporter v0.47.0"
  # - gomod: "github.com/open-telemetry/opentelemetry-collector-contrib/exporter/jaegerexporter v0.47.0"
  - gomod: "github.com/open-telemetry/opentelemetry-collector-contrib/exporter/splunkhecexporter v0.47.0"
  - gomod: confluent.io/kafkaexporter v0.0.0
    path: "/go/kafkaexporter/kafkaexporter"
extensions:
  - gomod: "github.com/open-telemetry/opentelemetry-collector-contrib/extension/healthcheckextension v0.47.0"
  - gomod: "github.com/open-telemetry/opentelemetry-collector-contrib/extension/pprofextension v0.47.0"
  - import: go.opentelemetry.io/collector/extension/zpagesextension
    gomod: go.opentelemetry.io/collector v0.47.0
receivers:
  - import: go.opentelemetry.io/collector/receiver/otlpreceiver
    gomod: go.opentelemetry.io/collector v0.47.0
processors:
  - import: go.opentelemetry.io/collector/processor/batchprocessor
    gomod: go.opentelemetry.io/collector v0.47.0
# replaces:
#   - cloud.google.com/go => cloud.google.com/go v1.5.0
EOF

# RUN builder --config=/go/otelcol-builder.yaml --output-path=/tmp/dist

COPY <<-"EOF" /go/otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
exporters:
  kafka:
    protocol_version: 2.6.0
    brokers:
      - "kafka:29092"
    topic: spans
    encoding: otlp_json
    retry_on_failure:
      enabled: true
      initial_interval: 5s
      max_interval: 30s
      max_elapsed_time: 120s
  prometheus:
    endpoint: "0.0.0.0:8889"
    namespace: promexample
    const_labels:
      label1: value1
  logging:
    loglevel: debug
  zipkin:
    endpoint: "http://zipkin-all-in-one:9411/api/v2/spans"
    format: proto
  jaeger:
    endpoint: jaeger-collector:14250
    tls:
      insecure: true
  splunk_hec:
    # Splunk HTTP Event Collector token.
    token: "abcd1234"
    # URL to a Splunk instance to send data to.
    endpoint: "https://splunk:8088/services/collector"
    # Optional Splunk source: https://docs.splunk.com/Splexicon:Source
    source: "otel"
    # Optional Splunk source type: https://docs.splunk.com/Splexicon:Sourcetype
    sourcetype: "otel"
    # Splunk index, optional name of the Splunk index targeted.
    #    index: "metrics"
    # Maximum HTTP connections to use simultaneously when sending data. Defaults to 100.
    max_connections: 200
    # Whether to disable gzip compression over HTTP. Defaults to false.
    disable_compression: false
    # HTTP timeout when sending data. Defaults to 10s.
    timeout: 10s
    tls:
      # Whether to skip checking the certificate of the HEC endpoint when sending data over HTTPS. Defaults to false.
      insecure_skip_verify: true
    # Application name is used to track telemetry information for Splunk App's using HEC by App name.
    splunk_app_name: "OpenTelemetry-Collector Splunk Exporter"
    # Application version is used to track telemetry information for Splunk App's using HEC by App version.
    splunk_app_version: "v0.0.1"
processors:
  batch:
extensions:
  health_check:
  pprof:
    endpoint: :1888
  zpages:
    endpoint: :55679
service:
  telemetry:
    logs:
      level: "debug"
  extensions: [pprof, zpages, health_check]
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging, zipkin, jaeger, splunk_hec, kafka]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging, prometheus]
EOF

COPY <<-"EOF" /tmp/otelcol.yaml
receivers:
  otlp:
    protocols:
      grpc:
exporters:
  kafka:
    protocol_version: 2.6.0
    brokers:
      - "kafka:29092"
    topic: spans
    encoding: otlp_json
    retry_on_failure:
      enabled: true
      initial_interval: 5s
      max_interval: 30s
      max_elapsed_time: 120s
  prometheus:
    endpoint: "0.0.0.0:8889"
    namespace: promexample
    const_labels:
      label1: value1
  logging:
    loglevel: debug
  zipkin:
    endpoint: "http://zipkin-all-in-one:9411/api/v2/spans"
    format: proto
  # jaeger:
  #   endpoint: jaeger-collector:14250
  #   tls:
  #     insecure: true
  splunk_hec:
    # Splunk HTTP Event Collector token.
    token: "abcd1234"
    # URL to a Splunk instance to send data to.
    endpoint: "https://splunk:8088/services/collector"
    # Optional Splunk source: https://docs.splunk.com/Splexicon:Source
    source: "otel"
    # Optional Splunk source type: https://docs.splunk.com/Splexicon:Sourcetype
    sourcetype: "otel"
    # Splunk index, optional name of the Splunk index targeted.
    #    index: "metrics"
    # Maximum HTTP connections to use simultaneously when sending data. Defaults to 100.
    max_connections: 200
    # Whether to disable gzip compression over HTTP. Defaults to false.
    disable_compression: false
    # HTTP timeout when sending data. Defaults to 10s.
    timeout: 10s
    tls:
      # Whether to skip checking the certificate of the HEC endpoint when sending data over HTTPS. Defaults to false.
      insecure_skip_verify: true
    # Application name is used to track telemetry information for Splunk App's using HEC by App name.
    splunk_app_name: "OpenTelemetry-Collector Splunk Exporter"
    # Application version is used to track telemetry information for Splunk App's using HEC by App version.
    splunk_app_version: "v0.0.1"
processors:
  batch:
extensions:
  health_check:
  pprof:
    endpoint: :1888
  zpages:
    endpoint: :55679
service:
  telemetry:
    logs:
      level: "debug"
  extensions: [pprof, zpages, health_check]
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging, zipkin, splunk_hec, kafka]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging, prometheus]
EOF

WORKDIR /go/kafkaexporter

RUN go mod download

# RUN go mod tidy

ENTRYPOINT [ "/go/run.sh" ]

CMD [ "--config", "/etc/otel/config.yaml" ]