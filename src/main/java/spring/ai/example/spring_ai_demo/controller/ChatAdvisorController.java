package spring.ai.example.spring_ai_demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class ChatAdvisorController {
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public ChatAdvisorController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(5).build();
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();

    }

    @GetMapping("/ai/advisor")
    String generation(@RequestParam(value = "message", defaultValue = "What day is today?") String userInput,
                      @RequestParam(value = "conversationId", defaultValue = "001") String conversationId) {

        String advisorPrompt = """
                <query>
                
                         Context information is below.
                
                 		---------------------
                <question_answer_context>
                 		---------------------
                
                Given the context information and no prior knowledge, answer the query.
                
                Follow these rules:
                
                1. If the answer is not in the context, just say that you don't know.
                2. Avoid statements like "Based on the context..." or "The provided information...".
                """;
        PromptTemplate customPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template(advisorPrompt)
                .build();

        Advisor safeguardAdvisor = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of(
                        "confidential",
                        "secret",
                        "internal",
                        "proprietary",
                        "classified",
                        "wine"
                ))
                .build();

        return this.chatClient.prompt()
                .user(userInput)
                .advisors(safeguardAdvisor)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .promptTemplate(customPromptTemplate)
                        .build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                //.tools(new WineTool(this.vectorStore))
                .call()
                .content();
    }
}



