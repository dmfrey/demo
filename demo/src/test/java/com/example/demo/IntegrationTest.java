package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.WebApplicationType.NONE;

@Testcontainers
public class IntegrationTest {

    @Container
    static RabbitMQContainer rabbitContainer =
            new RabbitMQContainer( "rabbitmq:3.8-alpine" );

    @Container
    static GenericContainer redisContainer =
            new GenericContainer( DockerImageName.parse( "redis:6-alpine" ) )
                    .withExposedPorts( 6379 );

    static Map<String, Object> properties = new HashMap<>();

    private static final UUID fakeId = UUID.randomUUID();
    private static final String fakeMessage = "test message";

    @BeforeAll
    static void initialize() {

        properties.put( "spring.rabbitmq.host", rabbitContainer.getHost() );
        properties.put( "spring.rabbitmq.port", rabbitContainer.getAmqpPort() );
        properties.put( "spring.rabbitmq.username", rabbitContainer.getAdminUsername() );
        properties.put( "spring.rabbitmq.password", rabbitContainer.getAdminPassword() );

        properties.put( "spring.redis.host", redisContainer.getContainerIpAddress() );
        properties.put( "spring.redis.port", redisContainer.getFirstMappedPort() );

    }

    @Test
    void test() {

        await().untilAsserted( () -> {
                assertThat( redisContainer.isRunning() ).isTrue();
                assertThat( rabbitContainer.isRunning() ).isTrue();
        });

        try( ConfigurableApplicationContext context = new SpringApplicationBuilder( DemoApplication.class )
                .web( NONE )
                .properties( properties )
                .run(
                        "--rabbit.supplier.queues=test.exchange.test",
                        "--rabbit.persistentDeliveryMode=true",
                        "--spring.rabbitmq.listener.simple.acknowledgeMode=AUTO",
                        "--spring.cloud.stream.bindings.listener-in-0.destination=test.exchange"
                )
        ) {

            RabbitTemplate rabbitTemplate = context.getBean( RabbitTemplate.class );
            rabbitTemplate.setMessageConverter( new Jackson2JsonMessageConverter() );

            rabbitTemplate.convertAndSend( "test.exchange", "", new Outgoing( fakeId, fakeMessage ) );
            await().atMost( 5, SECONDS ).untilAsserted( () -> {

                MessageEntityRepository repository = context.getBean( MessageEntityRepository.class );
                Optional<MessageEntity> actual = repository.findById( fakeId );

                assertThat( actual.isPresent() ).isTrue();
            });

        }

    }

}

class Outgoing implements Serializable {

    @JsonProperty( "id" )
    final UUID id;

    @JsonProperty( "message" )
    final String message;

    Outgoing(
            final UUID id,
            final String message
    ) {

        this.id = id;
        this.message = message;

    }

    UUID getId() {

        return this.id;
    }

    String getMessage() {

        return message;
    }

    @Override
    public String toString() {
        return "Incoming{" +
                "id=" + id +
                ", message='" + message + '\'' +
                '}';
    }

}
