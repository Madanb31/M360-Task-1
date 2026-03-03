package com.madan.M360_Task_1.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DocumentService {

    @Autowired
    private VectorStore vectorStore;

    public void ingestFile(Resource resource) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();

        TokenTextSplitter splitter = new TokenTextSplitter(
                400,   // defaultChunkSize (Keep < 512 for local embedding!)
                100,   // minChunkSizeChars (Don't keep tiny chunks)
                5,     // minChunkSizeToMerge
                10000, // maxNumChunks
                true   // keepSeparator
        ); // Splits long text
        List<Document> splitDocuments = splitter.apply(documents);

        vectorStore.add(splitDocuments);
    }
}