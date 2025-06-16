package spring.ai.example.spring_ai_demo.controller;


import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChromaDbController {

    private final VectorStore vectorStore;

    public ChromaDbController( VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping("/ai/chroma")
    public List<Document> query(@RequestParam(value = "message", defaultValue = "tasty wine") String message) {
        return this.vectorStore
                .similaritySearch(SearchRequest.builder().query(message).topK(3).build());

    }

    @GetMapping("/ai/chroma/meta")
    public List<Document> queryWithMeta(
            @RequestParam(value = "search_query", defaultValue = "tasty wine") String message,
            @RequestParam(value = "meta_query", defaultValue = "Grizzly Peak") String meta_query
    ) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("The World")
                        .topK(3)
                        .filterExpression("title == '"+ meta_query+"'").build());
    }
}
