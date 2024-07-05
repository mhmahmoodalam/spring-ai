package com.example.ai.localchat.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/rag-prompt-template.st")
    private Resource ragPromptTemplate;

    @Autowired
    public ChatController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/api/v1/chat")
    public String replyToChat(@RequestParam(value = "message", defaultValue = "introduce yourself") String message){
        var promptRequest = chatClient.prompt(new Prompt(new UserMessage(message)));
        return promptRequest.call().content();
    }

    @GetMapping("/api/v1/domain/chat")
    public String replyToDomainContextChat(@RequestParam(value = "message", defaultValue = "introduce yourself") String message){
        var similarDocuments = vectorStore.similaritySearch(SearchRequest.query(message).withTopK(3));
       var contentList= similarDocuments.stream().map(Document::getContent).toList();
       var promptTemplate = new PromptTemplate(ragPromptTemplate);
       var promptParameters = new HashMap<String,Object>();
       promptParameters.put("input",message);
       promptParameters.put("documents", String.join("\n",contentList));
        var promptRequest = chatClient.prompt(promptTemplate.create(promptParameters));
        return promptRequest.call().content();
    }
}
