// Copyright The OpenTelemetry Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package kafkaexporter // import "confluent.io/kafkaexporter"

import (
	"bytes"
	"fmt"

	"github.com/Shopify/sarama"
	"go.opentelemetry.io/collector/model/pdata"
)

type pdataLogsMarshaler struct {
	marshaler pdata.LogsMarshaler
	encoding  string
}

func (p pdataLogsMarshaler) Marshal(ld pdata.Logs, topic string) ([]*sarama.ProducerMessage, error) {
	bts, err := p.marshaler.MarshalLogs(ld)
	if err != nil {
		return nil, err
	}
	return []*sarama.ProducerMessage{
		{
			Topic: topic,
			Value: sarama.ByteEncoder(bts),
		},
	}, nil
}

func (p pdataLogsMarshaler) Encoding() string {
	return p.encoding
}

func newPdataLogsMarshaler(marshaler pdata.LogsMarshaler, encoding string) LogsMarshaler {
	return pdataLogsMarshaler{
		marshaler: marshaler,
		encoding:  encoding,
	}
}

type pdataMetricsMarshaler struct {
	marshaler pdata.MetricsMarshaler
	encoding  string
}

func (p pdataMetricsMarshaler) Marshal(ld pdata.Metrics, topic string) ([]*sarama.ProducerMessage, error) {
	bts, err := p.marshaler.MarshalMetrics(ld)
	if err != nil {
		return nil, err
	}
	return []*sarama.ProducerMessage{
		{
			Topic: topic,
			Value: sarama.ByteEncoder(bts),
		},
	}, nil
}

func (p pdataMetricsMarshaler) Encoding() string {
	return p.encoding
}

func newPdataMetricsMarshaler(marshaler pdata.MetricsMarshaler, encoding string) MetricsMarshaler {
	return pdataMetricsMarshaler{
		marshaler: marshaler,
		encoding:  encoding,
	}
}

type pdataTracesMarshaler struct {
	marshaler pdata.TracesMarshaler
	encoding  string
}

func (p pdataTracesMarshaler) Marshal(td pdata.Traces, topic string, span_map map[string]string) ([]*sarama.ProducerMessage, error) {
	buffers := make(map[string][]pdata.Span)
	rss := td.ResourceSpans()
	for i := 0; i < rss.Len(); i++ {
		rs := rss.At(i)

		ilss := rs.InstrumentationLibrarySpans()
		for j := 0; j < ilss.Len(); j++ {
			ils := ilss.At(j)

			spans := ils.Spans()
			for k := 0; k < spans.Len(); k++ {
				s := spans.At(k)
				trace_id := s.TraceID().HexString()
				arr := buffers[trace_id]
				if arr == nil {
					buffers[trace_id] = make([]pdata.Span, 0)
				}
				arr = append(arr, s)
				buffers[trace_id] = arr
			}
		}
	}

	messages := make([]*sarama.ProducerMessage, 0, len(buffers))

	for k, spans := range buffers {
		buf := bytes.Buffer{}
		fmt.Println("traceID:", k)
		found_spans, has_existing_trace := span_map[k]

		buf.WriteString("[")
		if has_existing_trace {
			buf.WriteString(found_spans[1 : len(found_spans)-1])
			if len(spans) > 0 {
				buf.WriteString(",")
			}
		}

		for i := 0; i < len(spans); i++ {
			s := spans[i]
			str := fmt.Sprintf("{\"traceId\":\"%s\",\"spanId\":\"%s\",\"name\":\"%s\",\"startTime\":\"%d\",\"endTime\":\"%d\",\"parentSpanId\":\"%s\",\"kind\":\"%s\"}", s.TraceID().HexString(), s.SpanID().HexString(), s.Name(), s.StartTimestamp(), s.EndTimestamp(), s.ParentSpanID().HexString(), s.Kind())
			buf.WriteString(str)
			if i < len(spans)-1 {
				buf.WriteString(",")
			}
		}
		buf.WriteString("]")

		messages = append(messages, &sarama.ProducerMessage{
			Topic: topic,
			Key:   sarama.StringEncoder(k),
			Value: sarama.ByteEncoder(buf.Bytes()),
		})

		// remove the trace from the map.
		if has_existing_trace {
			delete(span_map, k)
		}

		// TODO: The span_map will continue to grow indefinitely. Find a way to clean it up.
	}
	return messages, nil
}

func (p pdataTracesMarshaler) Encoding() string {
	return p.encoding
}

func newPdataTracesMarshaler(marshaler pdata.TracesMarshaler, encoding string) TracesMarshaler {
	return pdataTracesMarshaler{
		marshaler: marshaler,
		encoding:  encoding,
	}
}
