package com.madan.M360_Task_1.ai.tools;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagTools {

    @Autowired
    private VectorStore vectorStore;

    @Tool(description = "Searches company documents. Use this for questions about policies, contracts, company leadership (CEO, CTO), business terms, or general knowledge.")
    public String searchPolicyTool(@ToolParam(description = "The search query") String query) {

        // Search with threshold (0.7 means 70% match required)
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(3)           // Top 3 results
                        .similarityThreshold(0.3) // Filter weak matches
                        .build()
        );

        if (results.isEmpty()) {
            return "No relevant policy documents found.";
        }

        return results.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }
}