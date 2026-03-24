package com.madan.M360_Task_1.controllers;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Autowired
    EmbeddingModel embeddingModel;

    @GetMapping("/test-embed")
    public String test() {
        var vector = embeddingModel.embed("Hello");
        return "Embedded size: " + vector.length;
    }
}