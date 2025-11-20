package com.example.ai.localchat.controllers;

import com.example.ai.localchat.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/rag-prompt-template.st")
    private Resource ragPromptTemplate;

    @Autowired
    public ChatController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.clone().build();
        this.vectorStore = vectorStore;
        this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.50)
                        .topK(10)
                        .build())
                .build();
    }

    @GetMapping("/api/v1/chat/{conversationId}")
    public String replyToChat(@PathVariable String conversationId,
                              @RequestParam(value = "message", defaultValue = "introduce yourself") String message){
        var promptRequest = chatClient.prompt(new Prompt(new UserMessage(message)))
                .advisors(chatMemoryAdvisor, retrievalAugmentationAdvisor)
                .advisors(advisors -> advisors.param(
                        ChatMemory.CONVERSATION_ID, conversationId));
        return promptRequest.call().content();
    }

    @GetMapping("/api/v1/domain/chat/{conversationId}")
    public String replyToDomainContextChat(@PathVariable String conversationId,@RequestBody ChatRequest request){
        var similarDocuments = vectorStore.similaritySearch(SearchRequest.builder().query(request.getMessage())
                .topK(10).build());
       var contentList= similarDocuments.stream().map(Document::getFormattedContent).toList();
       var promptTemplate = new PromptTemplate(ragPromptTemplate);
       var promptParameters = new HashMap<String,Object>();
       promptParameters.put("input",request.getMessage());
       promptParameters.put("documents", String.join("\n",contentList));
        var promptRequest = chatClient.prompt(promptTemplate.create(promptParameters))
                .advisors(chatMemoryAdvisor)
                .advisors(advisors -> advisors.param(
                        ChatMemory.CONVERSATION_ID, conversationId));
        return promptRequest.call().content();
    }
}
