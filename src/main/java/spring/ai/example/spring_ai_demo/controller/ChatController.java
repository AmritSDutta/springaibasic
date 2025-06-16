package spring.ai.example.spring_ai_demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class MyController {
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public MyController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ai")
    String generation(@RequestParam(value = "message", defaultValue = "What day is today?") String userInput) {
        return this.chatClient.prompt()
                .user(userInput).tools(new WineTool(this.vectorStore))
                .call()
                .content();
    }
}

class WineTool {
    private final VectorStore vectorStore;

    WineTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }


    @Tool(name = "WineQuery", description = "Get the wine related details. Takes query string as input.")
    public List<Document> wineQuery(@ToolParam(description = "wine related query string") String query) {
        return this.vectorStore
                .similaritySearch(SearchRequest.builder().query(query).topK(3).build());

    }

}