package com.example.ai.localchat.readers;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;

import java.util.List;

public class AllDocumentTypeReader {

    private final Resource resource;

    public AllDocumentTypeReader(Resource resource) {
        this.resource = resource;
    }

    public List<Document> loadText() {
        /**
         * The TikaDocumentReader uses Apache Tika to extract text from a variety of document formats,
         * such as PDF, DOC/DOCX, PPT/PPTX, and HTML. For a comprehensive list of supported formats,
         * refer to the Tika documentation.
         */
        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.defaultConfig());
        return markdownDocumentReader.read();
    }
}
