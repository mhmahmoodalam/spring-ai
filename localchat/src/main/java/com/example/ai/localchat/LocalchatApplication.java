package com.example.ai.localchat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LocalchatApplication {

	public static void main(String[] args) {
		SpringApplication.run(LocalchatApplication.class, args);
	}

}
