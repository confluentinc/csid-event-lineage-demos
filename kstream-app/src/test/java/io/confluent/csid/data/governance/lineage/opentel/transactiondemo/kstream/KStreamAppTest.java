package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.kstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.csid.common.test.utils.KafkaConsumer;
import io.confluent.csid.common.test.utils.KafkaProducer;
import io.confluent.csid.common.test.utils.PropUtils;
import io.confluent.csid.common.test.utils.containers.KafkaCluster;
import io.confluent.csid.common.test.utils.containers.KafkaContainer;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class KStreamAppTest {

    @Test
    void runApp() {
        System.setProperty("app", "account-processor");
        KafkaCluster kafkaCluster = KafkaCluster.newCluster().withNodes(1);
        kafkaCluster.start();
        KafkaContainer container = kafkaCluster.getContainer(0);
        System.setProperty("bootstrapServer", container.getHost() + ":" + container.getPort());

        try {
            final KStreamApp app = new KStreamApp("account-processor-test", container.getHost() + ":" + container.getPort());
//            final KStreamApp app = new KStreamApp("account-processor-test", "localhost:29092");
            Thread thread = new Thread(() -> {
                try {
                    app.runStreams();
                    app.awaitFinish();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();


            Properties producerProperties = PropUtils.loadProperties("/producer.properties", KStreamAppTest.class);
            PropUtils.replace(producerProperties, "$host", container.getHost());
            PropUtils.replace(producerProperties, "$port", Integer.toString(container.getPort()));
//            PropUtils.replace(properties, "$host", "localhost");
//            PropUtils.replace(properties, "$port", "29092");
            KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);

            Properties consumerProperties = PropUtils.loadProperties("/consumer.properties", KStreamAppTest.class);
            PropUtils.replace(consumerProperties, "$host", container.getHost());
            PropUtils.replace(consumerProperties, "$port", Integer.toString(container.getPort()));
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);

            URL url = KStreamAppTest.class.getResource("/spans_json.txt");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(url.getFile()));

            String line = bufferedReader.readLine();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode on = objectMapper.readTree(line);
            int produceCount = 0;
            while (line != null) {
                producer.produce(on.get("traceId").asText(), line);
                line = bufferedReader.readLine();
                produceCount++;
            }

            Thread.sleep(2000);
            consumer.consumeRecords(2, produceCount);
            consumer.close();
            bufferedReader.close();
            producer.close();
            app.shutdown();
            app.awaitFinish();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            kafkaCluster.stop();
        }
    }

//    private static void replace(final Properties properties, final String key, final String value) {
//        properties.forEach((key1, value1) -> {
//            if (value1.toString().contains(key)) {
//                properties.put(key1, value1.toString().replace(key, value));
//            }
//        });
//    }
}
