package com.example.ai.localchat.service;

import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class HybridRetrievalService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    // Adjusted thresholds for technical content
    private static final double SIMILARITY_THRESHOLD = 0.65; // Lower for technical docs
    private static final int INITIAL_TOP_K = 20; // Cast wider net
    private static final int FINAL_TOP_K = 5; // Rerank to best 5

    public List<Document> hybridSearch(String query) {

        // Step 1: Query expansion for technical terms
        String expandedQuery = expandTechnicalQuery(query);

        // Step 2: Semantic search with lower threshold
        SearchRequest searchRequest = SearchRequest.builder()
                .query(expandedQuery)
                .topK(INITIAL_TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> candidateDocs = vectorStore.similaritySearch(searchRequest);

        if (candidateDocs.isEmpty()) {
            // Fallback: try without threshold
            searchRequest = SearchRequest.builder()
                    .query(expandedQuery)
                    .topK(INITIAL_TOP_K)
                    .build();
            candidateDocs = vectorStore.similaritySearch(searchRequest);
        }

        // Step 3: Metadata filtering
        List<Document> filteredDocs = applyMetadataFiltering(candidateDocs, query);

        // Step 4: Rerank using cross-encoder approach
        List<Document> rerankedDocs = rerank(query, filteredDocs);

        return rerankedDocs.stream()
                .limit(FINAL_TOP_K)
                .collect(Collectors.toList());
    }

    private String expandTechnicalQuery(String query) {
        // Use LLM to expand query with technical synonyms and context
        String expansionPrompt = """
            Expand this technical query by identifying:
            1. Key technical terms and their synonyms
            2. Related concepts
            3. Common variations (camelCase, snake_case, etc.)
            
            Original query: {query}
            
            Respond with only the expanded query terms, comma-separated.
            Keep it concise (max 50 words).
            """;

        PromptTemplate promptTemplate = new PromptTemplate(expansionPrompt);
        Prompt prompt = promptTemplate.create(Map.of("query", query));

        String expansion = chatClient.prompt(prompt)
                .call()
                .content();

        return query + " " + expansion;
    }

    private List<Document> applyMetadataFiltering(List<Document> docs, String query) {
        // Filter based on query characteristics
        boolean queryHasCode = query.toLowerCase().contains("code") ||
                query.toLowerCase().contains("example");

        return docs.stream()
                .filter(doc -> {
                    Map<String, Object> metadata = doc.getMetadata();

                    // If query mentions code, prioritize docs with code
                    if (queryHasCode && metadata.containsKey("has_code")) {
                        return (Boolean) metadata.get("has_code");
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Document> rerank(String query, List<Document> documents) {
        // Use LLM as reranker (cross-encoder style)
        String rerankPrompt = """
            Rate how relevant each document is to answering this query.
            Query: {query}
            
            Documents:
            {documents}
            
            Return only a comma-separated list of document indices (0-based) 
            in order of relevance (most relevant first).
            Example: 2,0,4,1,3
            """;

        StringBuilder docsBuilder = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            docsBuilder.append(String.format("[%d] %s\n\n", i,
                    documents.get(i).getFormattedContent().substring(0,
                            Math.min(300, documents.get(i).getFormattedContent().length()))));
        }

        PromptTemplate promptTemplate = new PromptTemplate(rerankPrompt);
        Prompt prompt = promptTemplate.create(Map.of(
                "query", query,
                "documents", docsBuilder.toString()
        ));

        String ranking = chatClient.prompt(prompt)
                .call()
                .content();

        // Parse ranking and reorder documents
        try {
            List<Integer> indices = Arrays.stream(ranking.trim().split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            return indices.stream()
                    .filter(i -> i >= 0 && i < documents.size())
                    .map(documents::get)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Fallback to original order
            return documents;
        }
    }
}

