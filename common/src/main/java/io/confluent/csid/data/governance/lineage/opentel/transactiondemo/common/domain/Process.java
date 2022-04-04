package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Process {
    String serviceName;
    List<Map<String, String>> tags;
}
