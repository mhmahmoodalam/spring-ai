package com.example.ai.localchat.config;

import com.example.ai.localchat.readers.AllDocumentTypeReader;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
public class RAGETLConfiguration {

    // define the vector store
    @Value("vectorstore.json")
    private String vectorStoreName;

    private static final int CHUNK_SIZE = 400; // Tokens for technical content
    private static final int CHUNK_OVERLAP = 80; // 20% overlap

    // define the embeding model
    // define the vector store (initialize by passing the embedding client)
    @Bean
    SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();
        File vectorStoreFile = getVectorStorFile();
        if(vectorStoreFile.exists()){
            simpleVectorStore.load(vectorStoreFile);
        }else{
            // use the file format redaer textRedaer/PdfPageReader etc to read the file
            // define a text splitter that will consume the redaer ouput ( Documents)
            // add the split dcouments to the vector store
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            try {
                Resource[] resources = resolver.getResources("classpath:/docs/knowledge/*.md");
                for (Resource resource : resources) {
                    AllDocumentTypeReader documentReader = new AllDocumentTypeReader(resource);
                    var documents = documentReader.loadText();
                    TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                            .withChunkSize(CHUNK_SIZE)
                            .withKeepSeparator(true)
                            .build();
                    var tokenizedDocuments = textSplitter.apply(documents).stream()
                            .map(this::enrichMetadata)
                            .collect(Collectors.toList());
                    simpleVectorStore.add(tokenizedDocuments);
                }
                simpleVectorStore.save(vectorStoreFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return simpleVectorStore;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private File getVectorStorFile() {
        var path = Paths.get("localchat","src","main", "resources","data");
        var absolutuePath = path.toFile().getAbsolutePath()+"\\"+vectorStoreName;
        return new File(absolutuePath);
    }

    private Document enrichMetadata(Document doc) {
        Map<String, Object> metadata = new HashMap<>(doc.getMetadata());

        // Extract technical context
        String content = doc.getFormattedContent();
        metadata.put("has_code", content.contains("```"));
        metadata.put("has_api_ref", content.matches(".*\\b(API|endpoint|method)\\b.*"));

        // Calculate text complexity for better ranking
        double complexity = calculateComplexity(content);
        metadata.put("complexity_score", complexity);

        // Extract key technical terms
        List<String> technicalTerms = extractTechnicalTerms(content);
        metadata.put("technical_terms", String.join(",", technicalTerms));

        return new Document(content, metadata);
    }

    private double calculateComplexity(String text) {
        // Simple complexity metric: code blocks, technical terms, sentence length
        double codeBlocks = (text.split("```").length - 1) * 2.0;
        double avgSentenceLength = Arrays.stream(text.split("[.!?]"))
                .mapToInt(String::length)
                .average()
                .orElse(0);

        return (codeBlocks + (avgSentenceLength / 100.0)) / 2.0;
    }

    private List<String> extractTechnicalTerms(String text) {
        // Extract camelCase, PascalCase, UPPER_CASE identifiers
        Pattern pattern = Pattern.compile("\\b([A-Z][a-z]+[A-Z][a-zA-Z]*|[A-Z_]{2,}|[a-z]+[A-Z][a-zA-Z]*)\\b");
        Matcher matcher = pattern.matcher(text);

        List<String> terms = new ArrayList<>();
        while (matcher.find()) {
            terms.add(matcher.group());
        }
        return terms;
    }





}
