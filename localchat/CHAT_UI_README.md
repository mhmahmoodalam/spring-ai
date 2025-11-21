# Spring AI Chat UI

This project provides a web-based chat interface for your Spring AI backend using a simple HTML/JavaScript frontend.

## Features

- **Simple Chat**: Basic chat functionality
- **Domain Context (RAG)**: Chat with Retrieval Augmented Generation using your knowledge base
- **Auto RAG**: Automatic RAG processing with the built-in advisor

## Prerequisites

1. **Ollama** running on `localhost:11434` with:
   - `llama3.2:latest` model for chat
   - `nomic-embed-text` model for embeddings

2. **Java 17+**

## How to Run

1. **Start Ollama** (if not already running):
   ```bash
   ollama serve
   ```

2. **Pull required models** (if not already installed):
   ```bash
   ollama pull llama3.2:latest
   ollama pull nomic-embed-text
   ```

3. **Build and run the application**:
   ```bash
   ./gradlew bootRun
   ```
   Or on Windows:
   ```cmd
   .\gradlew.bat bootRun
   ```

4. **Access the chat interface**:
   - Open your browser and go to: `http://localhost:8081`
   - You'll see a clean HTML chat interface

## Usage

1. **Select Chat Mode**: Choose from the dropdown:
   - **Domain Context (RAG)**: For questions about your knowledge base documents (recommended)
   - **Simple Chat**: For general conversations
   - **Auto RAG**: For automatic document retrieval and generation

2. **Start Chatting**: Type your message and press Enter or click Send

3. **View Responses**: The AI responses will appear in the chat interface with timestamps

## Architecture

- **Backend**: Spring AI with REST endpoints
- **Frontend**: Simple HTML/JavaScript (no complex frameworks)
- **Communication**: Direct API calls from JavaScript to Spring AI REST endpoints
- **Knowledge Base**: Your documents in `src/main/resources/docs/knowledge/`

## API Endpoints

The following REST endpoints are available and used by the UI:

- `GET /api/v1/chat/{conversationId}?message=` - Simple chat
- `POST /api/v1/domain/chat/{conversationId}` - Domain context chat
- `POST /api/v1/auto-rag/chat/{conversationId}` - Auto RAG chat

## Troubleshooting

1. **Make sure Ollama is running**: Check `http://localhost:11434`
2. **Verify models are installed**: Run `ollama list`
3. **Check application logs**: Look for any errors in the console
4. **Port conflicts**: The app now runs on port 8081 instead of 8080
5. **Browser issues**: Try refreshing the page or clearing browser cache