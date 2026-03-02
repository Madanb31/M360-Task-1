package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.service.DocumentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;

@RestController
@RequestMapping("/ai/docs")
public class RagController {

    private final ChatClient chatClient;
    @Autowired DocumentService documentService;
    @Autowired VectorStore vectorStore;

    public RagController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build()) // Use Builder!
                .build();
    }

    @PostMapping("/upload")
    public String upload(@RequestPart("file") MultipartFile file) throws IOException {
        documentService.ingestFile(file.getResource());
        return "Ingested!";
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String query) {
        return chatClient.prompt().user(query).call().content();
    }
}