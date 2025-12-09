package spring.ai.example.spring_ai_demo.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResult;
import org.springframework.ai.openai.OpenAiModerationModel;
import org.springframework.ai.openai.OpenAiModerationOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ChatWithModerationController {
    private static final Logger logger = LoggerFactory.getLogger(ChatWithModerationController.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final OpenAiModerationModel openAiModerationModel;
    private final OpenAiModerationOptions moderationOptions;

    public ChatWithModerationController(VectorStore vectorStore,
                                        ChatClient.Builder chatClientBuilder, OpenAiModerationModel openAiModerationModel) {
        this.openAiModerationModel = openAiModerationModel;

        this.moderationOptions = OpenAiModerationOptions.builder()
                .model("omni-moderation-latest")
                .build();
        ChatMemory chatMemory = MessageWindowChatMemory
                .builder().maxMessages(5)
                .build();
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.defaultAdvisors
                (MessageChatMemoryAdvisor.builder(chatMemory)
                        .build())
                .build();

    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403",
                    description = "Content violates safety policy"),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error")
    })
    @GetMapping("/ai/moderation")
    ResponseEntity<String> generation(@RequestParam(value = "message",
                              defaultValue = "What is wine?")
                      String userInput,
                                      @RequestParam(value = "conversationId",
                              defaultValue = "001")
                      String conversationId) {

        /* Moderation on Input */
        if(classifyModeration(userInput, true)){
            return ResponseEntity.status(403)
                    .body("Content violates safety policy");
        }

        var response =  this.chatClient.prompt("Please use advisors for " +
                        "answering wine related queries.")
                .user(userInput)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .build())
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        /* Moderation on Output */
        if(classifyModeration(response, false)) {
            return ResponseEntity.status(403)
                    .body("Content violates safety policy");
        }
        return ResponseEntity.ok(response);
    }

    private boolean classifyModeration(String corpus, boolean userPrompt){

        try {
            var moderationPrompt = new ModerationPrompt(corpus,
                    moderationOptions);
            var moderationResponse =
                    this.openAiModerationModel.call(moderationPrompt);
            var moderation = moderationResponse.getResult().getOutput();
            logger.info("moderation({}) flagged={} raw={}",
                    userPrompt ? "user" : "assistant",
                    moderation.getResults()
                            .stream()
                            .anyMatch(ModerationResult::isFlagged),
                    moderation.getResults()
                    );
            return moderation.getResults()
                    .stream()
                    .anyMatch(ModerationResult::isFlagged);
        } catch (Exception e) {
            logger.warn("moderation check failed - treating as safe=false", e);
            return true;
        }

    }
}



