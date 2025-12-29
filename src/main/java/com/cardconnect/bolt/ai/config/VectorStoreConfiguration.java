package com.cardconnect.bolt.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for Vector Store and RAG (Retrieval-Augmented Generation)
 * Uses Spring AI for embeddings and a simple in-memory store
 * Only enabled when AI prediction is enabled
 */
@Configuration
@ConditionalOnProperty(name = "ai.prediction.enabled", havingValue = "true")
public class VectorStoreConfiguration {

    /**
     * Simple in-memory document store for storing historical patterns
     */
    @Bean
    public SimpleDocumentStore documentStore() {
        return new SimpleDocumentStore();
    }

    /**
     * Simple in-memory document store implementation
     * Stores documents with their embeddings for similarity search
     */
    public static class SimpleDocumentStore {
        private final ConcurrentHashMap<String, StoredDocument> documents = new ConcurrentHashMap<>();

        public void add(String id, String content, float[] embedding) {
            documents.put(id, new StoredDocument(id, content, embedding, null, Instant.now()));
        }

        public void add(String content, float[] embedding) {
            add(java.util.UUID.randomUUID().toString(), content, embedding);
        }

        public String addDocument(String id, String content, String filename, float[] embedding) {
            String docId = id != null ? id : java.util.UUID.randomUUID().toString();
            documents.put(docId, new StoredDocument(docId, content, embedding, filename, Instant.now()));
            return docId;
        }

        public boolean removeDocument(String id) {
            return documents.remove(id) != null;
        }

        public StoredDocument getDocument(String id) {
            return documents.get(id);
        }

        public Collection<StoredDocument> getAllDocuments() {
            return documents.values();
        }

        public int getDocumentCount() {
            return documents.size();
        }

        public void clear() {
            documents.clear();
        }

        public List<String> similaritySearch(float[] queryEmbedding, int topK, double minScore) {
            List<ScoredDocument> scored = new ArrayList<>();

            for (StoredDocument doc : documents.values()) {
                double score = cosineSimilarity(queryEmbedding, doc.embedding());
                if (score >= minScore) {
                    scored.add(new ScoredDocument(doc.content(), score));
                }
            }

            scored.sort((a, b) -> Double.compare(b.score(), a.score()));

            return scored.stream()
                .limit(topK)
                .map(ScoredDocument::content)
                .toList();
        }

        private double cosineSimilarity(float[] a, float[] b) {
            if (a.length != b.length) return 0.0;

            double dotProduct = 0.0;
            double normA = 0.0;
            double normB = 0.0;

            for (int i = 0; i < a.length; i++) {
                dotProduct += a[i] * b[i];
                normA += a[i] * a[i];
                normB += b[i] * b[i];
            }

            double norm = Math.sqrt(normA) * Math.sqrt(normB);
            return norm == 0 ? 0.0 : dotProduct / norm;
        }

        public record StoredDocument(String id, String content, float[] embedding, String filename, Instant createdAt) {}
        private record ScoredDocument(String content, double score) {}
    }
}
