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

package model // import "confluent.io/kafkaexporter/model"

import (
	"bytes"
	"log"
	"reflect"
	"unsafe"

	"github.com/gogo/protobuf/jsonpb"

	// ipdata "go.opentelemetry.io/collector/model/internal/pdata"
	"go.opentelemetry.io/collector/model/pdata"
)

// NewJSONTracesMarshaler returns a model.TracesMarshaler. Marshals to OTLP json bytes.
func NewJSONTracesMarshaler() pdata.TracesMarshaler {
	return newJSONMarshaler()
}

// NewJSONMetricsMarshaler returns a model.MetricsMarshaler. Marshals to OTLP json bytes.
func NewJSONMetricsMarshaler() pdata.MetricsMarshaler {
	return newJSONMarshaler()
}

// NewJSONLogsMarshaler returns a model.LogsMarshaler. Marshals to OTLP json bytes.
func NewJSONLogsMarshaler() pdata.LogsMarshaler {
	return newJSONMarshaler()
}

type jsonMarshaler struct {
	delegate jsonpb.Marshaler
}

func newJSONMarshaler() *jsonMarshaler {
	return &jsonMarshaler{delegate: jsonpb.Marshaler{}}
}

func (e *jsonMarshaler) MarshalLogs(ld pdata.Logs) ([]byte, error) {
	log.Print("MarshalLogs")
	buf := bytes.Buffer{}
	log.Print(ld)
	// err := e.delegate.Marshal(&buf, ipdata.LogsToOtlp(ld))
	return buf.Bytes(), nil
}

// func printMap(buf *bytes.Buffer, v reflect.Value) {
// 	fields := reflect.TypeOf(v)
// 	values := reflect.ValueOf(v)
// 	for l := 0; l < fields.NumField(); l++ {
// 		field := fields.Field(l)
// 		value := values.Field(l)
// 		buf.WriteString(fmt.Sprintf("\"%s\":\"%s\"", field.Name, value))
// 		if (l + 1) < fields.NumField() {
// 			buf.WriteString(",")
// 		}
// 	}
// }

func (e *jsonMarshaler) MarshalMetrics(md pdata.Metrics) ([]byte, error) {
	log.Print("MarshalMetrics")

	buf := bytes.Buffer{}
	// err := e.delegate.Marshal(&buf, ipdata.MetricsToOtlp(md))
	return buf.Bytes(), nil
}

func GetUnexportedField(field reflect.Value) interface{} {
	return reflect.NewAt(field.Type(), unsafe.Pointer(field.UnsafeAddr())).Elem().Interface()
}

func (e *jsonMarshaler) MarshalTraces(td pdata.Traces) ([]byte, error) {
	log.Print("MarshalTraces")
	buf := bytes.Buffer{}
	// err := e.delegate.Marshal(&buf, ipdata.TracesToOtlp(td))
	return buf.Bytes(), nil
}

// func (e *jsonMarshaler) MarshalTraces(td pdata.Traces) (map[string][]byte, error) {
// 	log.Print("MarshalTraces")
// 	buffers := make(map[string][]Span)
// 	resp := make(map[string][]byte)

// 	// buf := bytes.Buffer{}
// 	// log.Print("orig")
// 	// log.Print(td.InternalRep().req)
// 	// va := reflect.ValueOf(&td).Elem()
// 	// unexported := va.Field(0)
// 	// orig := unexported.(*pdata.TracesData)
// 	// fmt.Println(unexported.Attributes())
// 	// log.Print(GetUnexportedField(origField))
// 	// buf.WriteString("[")
// 	rss := td.ResourceSpans()
// 	for i := 0; i < rss.Len(); i++ {
// 		rs := rss.At(i)

// 		ilss := rs.InstrumentationLibrarySpans()
// 		for j := 0; j < ilss.Len(); j++ {
// 			ils := ilss.At(j)

// 			spans := ils.Spans()
// 			for k := 0; k < spans.Len(); k++ {
// 				s := spans.At(k)
// 				trace_id := s.TraceID().HexString()
// 				arr := buffers[trace_id]
// 				if arr == nil {
// 					buffers[trace_id] = make([]Span, 0)
// 				}
// 				append(arr, s)
// 				buffers[trace_id] = arr
// 			}
// 		}
// 	}

// 	for k, s := range buffers {
// 		buf := bytes.Buffer{}
// 		buf.WriteString("[")
// 		buf.WriteString(fmt.Sprintf("{\"traceId\":\"%s\",\"spanId\":\"%s\",\"name\":\"%s\",\"startTime\":%d,\"endTime\":%d,\"parentSpanId\":\"%s\",\"kind\":\"%s\"}", s.TraceID().HexString(), s.SpanID().HexString(), s.Name(), s.StartTimestamp(), s.EndTimestamp(), s.ParentSpanID().HexString(), s.Kind()))
// 		buf.WriteString("]")
// 		resp[k] = buf.Bytes()
// 	}

// 	// buf.WriteString("[")
// 	// rss := td.ResourceSpans()
// 	// for i := 0; i < rss.Len(); i++ {
// 	// 	rs := rss.At(i)

// 	// 	ilss := rs.InstrumentationLibrarySpans()
// 	// 	for j := 0; j < ilss.Len(); j++ {
// 	// 		ils := ilss.At(j)

// 	// 		spans := ils.Spans()
// 	// 		for k := 0; k < spans.Len(); k++ {
// 	// 			s := spans.At(k)
// 	// 			trace_id := s.TraceID().HexString()
// 	// 			arr := buffers[trace_id]
// 	// 			if arr == nil {
// 	// 				buffers[trace_id] = make([]byte[])
// 	// 			}
// 	// 			append(buffers[trace_id], s)
// 	// 			// buf.WriteString(fmt.Sprintf("%s,", s.Proto()))
// 	// 			buf.WriteString(fmt.Sprintf("{\"traceId\":\"%s\",\"spanId\":\"%s\",\"name\":\"%s\",\"startTime\":%d,\"endTime\":%d,\"parentSpanId\":\"%s\",\"kind\":\"%s\"}", s.TraceID().HexString(), s.SpanID().HexString(), s.Name(), s.StartTimestamp(), s.EndTimestamp(), s.ParentSpanID().HexString(), s.Kind()))
// 	// 			// attrs := s.Attributes()
// 	// 			// raw_attrs := s.Attributes()
// 	// 			// attrs := reflect.ValueOf(&raw_attrs).Elem().Field(0)

// 	// 			// fmt.Println(attrs)
// 	// 			// fields := reflect.TypeOf(attrs)
// 	// 			// values := reflect.ValueOf(attrs)
// 	// 			// for l := 0; l < fields.NumField(); l++ {
// 	// 			// 	field := fields.Field(l)
// 	// 			// 	value := values.Field(l)
// 	// 			// 	// printMap(&buf, value)
// 	// 			// 	// fmt.Println("attrs")
// 	// 			// 	// fmt.Println(GetUnexportedField(value.Elem()))
// 	// 			// 	buf.WriteString(fmt.Sprintf("\"%s\":\"%s\"", field.Name, value))
// 	// 			// 	if (l + 1) < fields.NumField() {
// 	// 			// 		buf.WriteString(",")
// 	// 			// 	}
// 	// 			// }
// 	// 			// buf.WriteString("}}")
// 	// 			if (k + 1) < spans.Len() {
// 	// 				buf.WriteString(",")
// 	// 			}
// 	// 			// log.Print(s)
// 	// 			// err := e.delegate.Marshal(&buf, ipdata.SpanToOtlp(s))
// 	// 			// if err != nil {
// 	// 			// 	log.Print(err)
// 	// 			// }
// 	// 			// buf.WriteString(",")
// 	// 		}
// 	// 	}
// 	// }
// 	// buf.WriteString("]")
// 	// log.Print(td.ResourceSpans().At(0).InstrumentationLibrarySpans().At(0).Spans().At(0).Name())
// 	// err := e.delegate.Marshal(&buf, td.InternalRep().req)
// 	// log.Print(fmt.Sprintf("Buffer: %s", buf.String()))
// 	// return buf.Bytes(), nil
// 	return resp, nil
// }
