package spring.ai.example.spring_ai_demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrchestratorController {
    private final RecurrenceWorkflow workflow;

    public OrchestratorController(VectorStore vectorStore,
                                  ChatClient.Builder chatClientBuilder) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(5)
                .build();

        Advisor safeguardAdvisor = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of(
                        "alcohol"
                ))
                .build();
        ChatClient chatClient = chatClientBuilder.defaultAdvisors(
                MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .build(),
                safeguardAdvisor
        ).build();
        workflow = new RecurrenceWorkflow(chatClient);
    }

    @GetMapping("/ai/chain")
    String generation(@RequestParam(value = "message",
                              defaultValue = "What is wine?")
                      String userInput,
                      @RequestParam(value = "conversationId",
                              defaultValue = "001")
                      String conversationId) {


        return this.workflow.chain(userInput);
    }
}

class RecurrenceWorkflow {
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";

    private static final String[] prompts = {
            // Step 1
            """
					Extract wine details from advisors.
					Format each piece of data.""",

            // Step 2
            """
					from the wine details find out details of its origin location.
					Add those location details to the wine details.""",
            // Step 3
            """
					Summarize all details together.
					Answer should be elaborated and to the point.it should be within 250 words""",
    };

    private final ChatClient chatClient;

    private final String[] systemPrompts;

    public RecurrenceWorkflow(ChatClient chatClient) {
        this(chatClient, prompts);
    }

    public RecurrenceWorkflow(ChatClient chatClient, String[] systemPrompts) {
        this.chatClient = chatClient;
        this.systemPrompts = systemPrompts;
    }

    public String chain(String userInput) {

        int step = 0;
        String response = userInput;
        System.out.printf(GREEN + "User asked:\n%s%n", response);

        //loop for all prompts
        for (String prompt : systemPrompts) {
            String input = String.format("{%s}\n {%s}", prompt, response);
            System.out.println(PURPLE + input);
            response = this.chatClient.prompt(input).call().content();
            System.out.println(BLUE + String.format("\nSTEP %s:\n %s", step++, response));
        }

        return response;
    }
}