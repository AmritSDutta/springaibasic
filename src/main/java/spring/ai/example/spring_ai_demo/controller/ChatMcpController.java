package spring.ai.example.spring_ai_demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
class ChatMcpController {
    private static final Logger logger = LoggerFactory.getLogger(ChatMcpController.class);
    private final ChatClient chatClient;
    //private final ToolCallbackProvider mcpToolProvider;

    public ChatMcpController(VectorStore vectorStore,
                             ChatClient.Builder chatClientBuilder
            //, ToolCallbackProvider mcpToolProvider
                             ) {
        //this.mcpToolProvider = mcpToolProvider;

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(5)
                .build();
        this.chatClient = chatClientBuilder.defaultAdvisors(
                MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .build()
        ).build();
    }

    @GetMapping("/ai/mcp/wine_explore")
    Mono<String> generation(@RequestParam(value = "message",
                                    defaultValue = "What wine do you suggest me?")
                            String userInput,
                            @RequestParam(value = "conversationId",
                                    defaultValue = "001")
                            String conversationId) {

        //logger.info("mcpToolProvider class = {}", mcpToolProvider.getClass().getName());

        return Mono.fromCallable(() ->
                        chatClient.prompt()
                                .system("""
                                Please use provide tolls for wine related queries
                                """)
                                .user(userInput)
                                .advisors(new SimpleLoggerAdvisor())
                                /* MCP tool callbacks added*/
                                //.toolCallbacks(this.mcpToolProvider)
                                .call()
                                .content()
                )
                .doOnNext(logger::info)
                .subscribeOn(
                        reactor.core.scheduler.Schedulers.boundedElastic()
                )
                .timeout(Duration.ofSeconds(30),
                        Mono.just("{\"error\":\"timeout\"}"));
    }
}

