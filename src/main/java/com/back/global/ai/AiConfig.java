package com.back.global.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    // ChatMemory는 대화의 상태를 저장하고 관리하는 객체입니다.
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder() // MessageWindowChatMemory는 대화의 메시지를 저장하는 메모리입니다.
                .chatMemoryRepository(chatMemoryRepository) // 대화의 상태를 저장하는 저장소입니다.
                .maxMessages(10) // 최대 메시지 수를 10으로 설정합니다.
                .build();
    }

    // ChatClient는 AI와의 대화를 관리하는 클라이언트입니다.
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()) // 대화의 상태를 관리하는 어드바이저입니다.
                .build();
    }
}