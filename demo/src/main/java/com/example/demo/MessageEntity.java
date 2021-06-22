package com.example.demo;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.UUID;

@RedisHash( "messages" )
@Data
@Builder
public class MessageEntity {

    @Id
    private UUID id;

    private String message;

}
