package com.example.demo;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface MessageEntityRepository extends CrudRepository<MessageEntity, UUID> {
}
