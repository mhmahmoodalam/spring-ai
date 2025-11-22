package com.example.ai.localchat.service;

import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class TechnicalRAGService {

    private final ChatClient chatClient;

    private final Advisor optimizedRagAdvisor;

    private static final String TECHNICAL_SYSTEM_PROMPT = """
        You are a technical documentation assistant specializing in {domain}.
        
        RESPONSE GUIDELINES:
        1. Answer directly and concisely
        2. Use technical terminology naturally
        3. Include code examples when relevant
        4. Structure your answer with markdown formatting
        5. NEVER mention "based on the provided documents" or similar phrases
        6. If information is insufficient, state what you can answer and what's unclear
        7. Format code blocks with proper language identifiers
        
        CRITICAL: Respond as if this information is your native knowledge.
        Focus on the answer, not the source of information.
        """;

    private static final String USER_PROMPT_TEMPLATE = """
        {question}
        
        Context information:
        {context}
        
        Provide a clear, technical answer formatted in markdown.
        """;

    public String queryWithRAG(String question, String domain) {

        SystemPromptTemplate systemPromptTemplate =
                new SystemPromptTemplate(TECHNICAL_SYSTEM_PROMPT);
        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("domain", domain)
        );

        return chatClient.prompt()
                .system(systemMessage.getText())
                .advisors(advisorSpec -> advisorSpec
                        .advisors(optimizedRagAdvisor)
                        .param("question", question))
                .user(question)
                .call()
                .content();
    }
}
