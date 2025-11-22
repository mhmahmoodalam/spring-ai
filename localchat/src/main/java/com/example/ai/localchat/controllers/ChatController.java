package com.example.ai.localchat.controllers;

import com.example.ai.localchat.ChatRequest;
import com.example.ai.localchat.service.HybridRetrievalService;
import com.example.ai.localchat.service.TechnicalRAGService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;
    private final VectorStore vectorStore;
    private final TechnicalRAGService ragService;
    private final HybridRetrievalService retrievalService;

    @Value("classpath:/prompts/rag-prompt-template.st")
    private Resource ragPromptTemplate;

    @Autowired
    public ChatController(
            ChatClient chatClient,
            ChatMemory chatMemory,
            VectorStore vectorStore,
            TechnicalRAGService ragService,
            HybridRetrievalService retrievalService) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.ragService = ragService;
        this.retrievalService= retrievalService;
        this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.3)  // Lower threshold for better recall with smaller models
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

    @PostMapping("/api/v1/domain/chat/{conversationId}")
    public String replyToDomainContextChat(@PathVariable String conversationId,@RequestBody ChatRequest request){
        // Use a lower similarity threshold for better retrieval with smaller models
        var searchRequest = SearchRequest.builder()
                .query(request.getMessage())
                .topK(20)
                .similarityThreshold(0.3)  // Lower threshold for better recall
                .build();
        
        var similarDocuments = vectorStore.similaritySearch(searchRequest);
        
        // Debug logging
        System.out.println("Query: " + request.getMessage());
        System.out.println("Found " + similarDocuments.size() + " similar documents");
        
        if (similarDocuments.isEmpty()) {
            System.out.println("No documents found with similarity threshold 0.3");
            // Try without similarity threshold
            var fallbackRequest = SearchRequest.builder()
                    .query(request.getMessage())
                    .topK(20)
                    .build();
            similarDocuments = vectorStore.similaritySearch(fallbackRequest);
            System.out.println("Fallback search found " + similarDocuments.size() + " documents");
        }
        
        var contentList = similarDocuments.stream()
                .peek(doc -> System.out.println("Document content preview: " + 
                    doc.getFormattedContent().substring(0, Math.min(100, doc.getFormattedContent().length())) + "..."))
                .map(Document::getFormattedContent)
                .toList();
                
        var promptTemplate = new PromptTemplate(ragPromptTemplate);
        var promptParameters = new HashMap<String,Object>();
        promptParameters.put("input", request.getMessage());
        promptParameters.put("documents", String.join("\n\n", contentList));
        
        var promptRequest = chatClient.prompt(promptTemplate.create(promptParameters))
                .advisors(chatMemoryAdvisor)
                .advisors(advisors -> advisors.param(
                        ChatMemory.CONVERSATION_ID, conversationId));
        return promptRequest.call().content();
    }

    @PostMapping("/api/v1/auto-rag/chat/{conversationId}")
    public ResponseEntity<String> replyWithAutoRAG(@PathVariable String conversationId, @RequestBody ChatRequest request){


        // Step 1: Retrieve relevant documents
        List<Document> documents = retrievalService.hybridSearch(request.getMessage());

        // Step 2: Generate answer with RAG
        String answer = ragService.queryWithRAG(
                request.getMessage(),
                ""
        );

        return ResponseEntity.ok(answer);
    }

    @GetMapping("/api/v1/test/documents")
    public String testDocumentRetrieval(@RequestParam(value = "query", defaultValue = "test") String query){
        var searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .build();
        
        var documents = vectorStore.similaritySearch(searchRequest);
        StringBuilder result = new StringBuilder();
        result.append("Found ").append(documents.size()).append(" documents for query: '").append(query).append("'\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            var doc = documents.get(i);
            result.append("Document ").append(i + 1).append(":\n");
            result.append("Content preview: ").append(doc.getFormattedContent().substring(0, Math.min(200, doc.getFormattedContent().length()))).append("...\n");
            result.append("Metadata: ").append(doc.getMetadata()).append("\n\n");
        }
        
        return result.toString();
    }

}