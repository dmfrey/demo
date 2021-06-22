package com.example.demo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class MessageListenerConfig {

    @Bean
    @Transactional
    public Consumer<Message<Incoming>> listener( final MessageEntityRepository repository ) {

        return input -> {
            log.debug( "listener : message=[{}]", input );

            MessageEntity created = repository.save(
                    MessageEntity.builder()
                            .id( input.getPayload().getId() )
                            .message( input.getPayload().getMessage() )
                            .build()
            );
            log.debug( "listener : created=[{}]", created );

        };

    }

}

@JsonIgnoreProperties( ignoreUnknown = true )
class Incoming {

    final UUID id;
    final String message;

    @JsonCreator
    Incoming(
            @JsonProperty( "id" ) final UUID id,
            @JsonProperty( "message" ) final String message
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
