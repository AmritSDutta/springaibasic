package spring.ai.example.spring_ai_demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SimpleChatController {
    private final ChatClient chatClient;

    public SimpleChatController(ChatClient.Builder chatClientBuilder) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(5)
                .build(); // remember last 5 conversations
        this.chatClient = chatClientBuilder.defaultAdvisors(
                MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .build()
        ).build();
    }

    @GetMapping("/ai/chat")
    String generation(@RequestParam(value = "message",
                              defaultValue = "Hello LLM") String userInput,
                      @RequestParam(value = "conversationId",
                              defaultValue = "001") String conversationId) {
        return this.chatClient.prompt("provide succinct answers.")
                .user(userInput)
                .advisors(a ->
                        a.param(
                                ChatMemory.CONVERSATION_ID
                                ,conversationId))
                .call()
                .content();
    }
}

