package com.example.ai.localchat.config;

import com.example.ai.localchat.service.HybridRetrievalService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class RAGConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder){
        return chatClientBuilder.clone().build();
    }

    @Bean
    public Advisor optimizedRagAdvisor(
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder,
            HybridRetrievalService hybridRetrievalService) {

        // Custom document retriever using hybrid search
        DocumentRetriever customRetriever = new DocumentRetriever() {
            @Override
            public List<Document> retrieve(Query query) {
                return hybridRetrievalService.hybridSearch(query.text());
            }
        };

        // Query transformer for technical queries
        QueryTransformer technicalQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .promptTemplate(new PromptTemplate("""
                Rewrite this query to better retrieve technical documentation.
                Focus on:
                - Extracting key technical terms
                - Identifying the core question
                - Adding relevant technical context
                
                Original query: {query}
                
                Rewritten query: {target}
                """))
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(customRetriever)
                .queryTransformers(technicalQueryTransformer)
                .build();
    }

    private List<Document> formatDocumentsForContext(List<Document> documents) {
        // Remove duplicate information and format for better context
        return documents.stream()
                .map(doc -> {
                    String content = doc.getFormattedContent();

                    // Clean up markdown for better LLM consumption
                    // content = content.replaceAll("``````\n"); // Normalize code blocks
                    content = content.replaceAll("\\n{3,}", "\n\n"); // Remove excessive newlines

                    return new Document(content, doc.getMetadata());
                })
                .distinct()
                .collect(Collectors.toList());
    }
}

