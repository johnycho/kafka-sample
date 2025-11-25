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
    topics = {"test-topic"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext
class KafkaIntegrationTest {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void testKafkaProducerAndConsumer() throws Exception {
        // Given
        String testTopic = "test-topic";
        String testMessage = "안녕하세요, 임베디드 카프카 테스트입니다!";

        // 테스트용 컨슈머 설정
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(testTopic);
        
        BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();
        
        containerProperties.setMessageListener((MessageListener<String, String>) record -> {
            System.out.println("테스트 컨슈머가 메시지를 받았습니다: " + record.value());
            records.add(record);
        });

        KafkaMessageListenerContainer<String, String> container = 
            new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        container.start();
        
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // When - 메시지 전송
        kafkaProducer.sendMessage(testTopic, testMessage);

        // Then - 메시지 수신 확인
        ConsumerRecord<String, String> received = records.poll(10, TimeUnit.SECONDS);
        
        assertThat(received).isNotNull();
        assertThat(received.value()).isEqualTo(testMessage);
        assertThat(received.topic()).isEqualTo(testTopic);

        container.stop();
    }

    @Test
    void testKafkaProducerWithKey() throws Exception {
        // Given
        String testTopic = "test-topic";
        String testKey = "test-key-1";
        String testMessage = "키를 가진 메시지 테스트";

        // 테스트용 컨슈머 설정
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-2");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(testTopic);
        
        BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();
        
        containerProperties.setMessageListener((MessageListener<String, String>) record -> {
            System.out.println("테스트 컨슈머가 메시지를 받았습니다 - Key: " + record.key() + ", Value: " + record.value());
            records.add(record);
        });

        KafkaMessageListenerContainer<String, String> container = 
            new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        container.start();
        
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // When - 키와 함께 메시지 전송
        kafkaProducer.sendMessage(testTopic, testKey, testMessage);

        // Then - 메시지와 키 수신 확인
        ConsumerRecord<String, String> received = records.poll(10, TimeUnit.SECONDS);
        
        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo(testKey);
        assertThat(received.value()).isEqualTo(testMessage);
        assertThat(received.topic()).isEqualTo(testTopic);

        container.stop();
    }

    @Test
    void testMultipleMessages() throws Exception {
        // Given
        String testTopic = "test-topic";
        int messageCount = 5;

        // 테스트용 컨슈머 설정
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-3");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(testTopic);
        
        BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();
        
        containerProperties.setMessageListener((MessageListener<String, String>) record -> {
            System.out.println("테스트 컨슈머가 메시지를 받았습니다: " + record.value());
            records.add(record);
        });

        KafkaMessageListenerContainer<String, String> container = 
            new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        container.start();
        
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // When - 여러 메시지 전송
        for (int i = 0; i < messageCount; i++) {
            kafkaProducer.sendMessage(testTopic, "메시지 " + i);
        }

        // Then - 모든 메시지 수신 확인
        for (int i = 0; i < messageCount; i++) {
            ConsumerRecord<String, String> received = records.poll(10, TimeUnit.SECONDS);
            assertThat(received).isNotNull();
            assertThat(received.value()).contains("메시지");
        }

        assertThat(records).isEmpty();

        container.stop();
    }
}


