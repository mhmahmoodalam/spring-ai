package com.example.ai.localchat;


public class ChatRequest {
    private String message;


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if(message != null){
            this.message = message;

        }else {
            this.message="introduce yourself";
        }
    }
}
