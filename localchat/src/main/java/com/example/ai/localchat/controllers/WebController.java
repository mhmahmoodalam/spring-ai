package com.example.ai.localchat.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "chat";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
}