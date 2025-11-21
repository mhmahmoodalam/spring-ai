package com.example.ai.localchat.config;

import com.example.ai.localchat.readers.AllDocumentTypeReader;
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

@Configuration
public class RAGETLConfiguration {

    // define the vector store
    @Value("vectorstore.json")
    private String vectorStoreName;

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
                    TokenTextSplitter textSplitter = new TokenTextSplitter();
                    var tokenizedDocuments = textSplitter.apply(documents);
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





}
