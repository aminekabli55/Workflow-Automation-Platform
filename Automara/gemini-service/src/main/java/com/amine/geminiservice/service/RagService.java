package com.amine.geminiservice.service;


import com.pgvector.PGvector;
import com.amine.geminiservice.model.DocumentEmbedding;
import com.amine.geminiservice.repository.EmbeddingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class RagService {

    @Autowired
    private EmbeddingRepository embeddingRepository;

    @Autowired
    private GeminiApiClient geminiApiClient;

    private static final int CHUNK_SIZE = 500; // characters per chunk
    private static final int CHUNK_OVERLAP = 50; // overlap between chunks

    /**
     * Embed a document by splitting into chunks and storing embeddings
     */
    public int embedDocument(String userId, String documentId, String text) {
        try {
            // Split text into chunks
            List<String> chunks = splitIntoChunks(text, CHUNK_SIZE, CHUNK_OVERLAP);

            log.info("Embedding {} chunks for document {}", chunks.size(), documentId);

            // Generate embeddings for each chunk
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                // Generate embedding
                float[] embedding = geminiApiClient.generateEmbedding(chunk);

                // Store in database
                DocumentEmbedding docEmbedding = new DocumentEmbedding();
                docEmbedding.setUserId(userId);
                docEmbedding.setDocumentId(documentId);
                docEmbedding.setChunkText(chunk);
                docEmbedding.setEmbedding(new PGvector(embedding));

                embeddingRepository.save(docEmbedding);
            }

            return chunks.size();

        } catch (Exception e) {
            log.error("Error embedding document: {}", e.getMessage(), e);
            throw new RuntimeException("Document embedding failed", e);
        }
    }

    /**
     * Query with RAG - find relevant context and generate answer
     */
    public String queryWithContext(String userId, String query, int topK) {
        try {
            // Generate query embedding
            float[] queryEmbedding = geminiApiClient.generateEmbedding(query);

            // Convert to string format for PostgreSQL
            String embeddingStr = vectorToString(queryEmbedding);

            // Find similar chunks
            List<DocumentEmbedding> similar = embeddingRepository.findSimilar(
                    userId,
                    embeddingStr,
                    topK
            );

            if (similar.isEmpty()) {
                return geminiApiClient.generateText(query);
            }

            // Build context from similar chunks
            StringBuilder context = new StringBuilder("Context:\n");
            for (DocumentEmbedding embedding : similar) {
                context.append("- ").append(embedding.getChunkText()).append("\n");
            }
            context.append("\nQuestion: ").append(query);
            context.append("\nAnswer based on the context above:");

            // Generate answer with context
            return geminiApiClient.generateText(context.toString());

        } catch (Exception e) {
            log.error("Error in RAG query: {}", e.getMessage(), e);
            throw new RuntimeException("RAG query failed", e);
        }
    }

    /**
     * Split text into chunks with overlap
     */
    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += (chunkSize - overlap)) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));

            if (end == text.length()) break;
        }

        return chunks;
    }

    /**
     * Convert float array to PostgreSQL vector string format
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}