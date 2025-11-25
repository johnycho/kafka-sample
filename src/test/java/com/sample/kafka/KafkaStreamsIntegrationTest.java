package com.sample.kafka;

import com.sample.kafka.producer.KafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "input-topic",
        "output-topic",
        "filter-input-topic",
        "filter-output-topic",
        "word-input-topic",
        "word-count-output-topic"
    },
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext
class KafkaStreamsIntegrationTest {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void testUpperCaseStream() throws Exception {
        // Given
        String inputMessage = "hello kafka streams";
        String expectedOutput = "HELLO KAFKA STREAMS";

        BlockingQueue<ConsumerRecord<String, String>> records = setupConsumer("output-topic", "test-output-group");

        // When - 입력 토픽으로 메시지 전송
        Thread.sleep(2000); // 스트림즈가 시작될 시간 대기
        kafkaProducer.sendMessage("input-topic", inputMessage);

        // Then - 출력 토픽에서 대문자로 변환된 메시지 확인
        ConsumerRecord<String, String> received = records.poll(10, TimeUnit.SECONDS);
        
        assertThat(received).isNotNull();
        assertThat(received.value()).isEqualTo(expectedOutput);
    }

    @Test
    void testFilterStream() throws Exception {
        // Given
        String importantMessage = "중요한 메시지입니다";
        String normalMessage = "일반 메시지입니다";

        BlockingQueue<ConsumerRecord<String, String>> records = setupConsumer("filter-output-topic", "test-filter-group");

        // When
        Thread.sleep(2000);
        kafkaProducer.sendMessage("filter-input-topic", importantMessage);
        kafkaProducer.sendMessage("filter-input-topic", normalMessage);

        // Then - '중요' 키워드가 있는 메시지만 통과
        ConsumerRecord<String, String> received = records.poll(10, TimeUnit.SECONDS);
        
        assertThat(received).isNotNull();
        assertThat(received.value()).contains("중요");
        
        // 일반 메시지는 필터링되어 받지 못함
        ConsumerRecord<String, String> shouldBeNull = records.poll(3, TimeUnit.SECONDS);
        assertThat(shouldBeNull).isNull();
    }

    @Test
    void testWordCountStream() throws Exception {
        // Given
        String sentence = "hello world hello";

        BlockingQueue<ConsumerRecord<String, String>> records = setupConsumer("word-count-output-topic", "test-word-count-group");

        // When
        Thread.sleep(2000);
        kafkaProducer.sendMessage("word-input-topic", sentence);

        // Then - 각 단어의 카운트 확인
        // hello: 1
        ConsumerRecord<String, String> record1 = records.poll(10, TimeUnit.SECONDS);
        assertThat(record1).isNotNull();
        
        // world: 1
        ConsumerRecord<String, String> record2 = records.poll(5, TimeUnit.SECONDS);
        assertThat(record2).isNotNull();
        
        // hello: 2 (누적)
        ConsumerRecord<String, String> record3 = records.poll(5, TimeUnit.SECONDS);
        assertThat(record3).isNotNull();
        
        System.out.println("단어 카운트 결과:");
        System.out.println(record1.key() + ": " + record1.value());
        System.out.println(record2.key() + ": " + record2.value());
        System.out.println(record3.key() + ": " + record3.value());
    }

    private BlockingQueue<ConsumerRecord<String, String>> setupConsumer(String topic, String groupId) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(topic);
        
        BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();
        
        containerProperties.setMessageListener((MessageListener<String, String>) record -> {
            System.out.println("테스트 컨슈머가 메시지를 받았습니다 - Topic: " + topic + ", Key: " + record.key() + ", Value: " + record.value());
            records.add(record);
        });

        KafkaMessageListenerContainer<String, String> container = 
            new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        container.start();
        
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        
        return records;
    }
}


