package com.cardconnect.bolt.ai.controller;

import com.cardconnect.bolt.ai.config.VectorStoreConfiguration.SimpleDocumentStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for RAG Document Management
 * Allows uploading, viewing, and deleting documents from the vector store
 */
@RestController
@RequestMapping("/api/rag")
@Slf4j
@Tag(name = "RAG Documents", description = "Manage documents in the RAG vector store for AI context enrichment")
public class RAGDocumentController {

    private final Optional<SimpleDocumentStore> documentStore;

    @Autowired
    public RAGDocumentController(@Autowired(required = false) SimpleDocumentStore documentStore) {
        this.documentStore = Optional.ofNullable(documentStore);
    }

    /**
     * GET /api/rag/documents
     * List all documents in the vector store
     */
    @GetMapping("/documents")
    @Operation(
        summary = "List All Documents",
        description = "Returns a list of all documents stored in the RAG vector store"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "RAG service not available")
    })
    public ResponseEntity<DocumentListResponse> listDocuments() {
        if (documentStore.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new DocumentListResponse(List.of(), 0, "RAG service is not enabled"));
        }

        try {
            List<DocumentInfo> documents = documentStore.get().getAllDocuments().stream()
                .map(doc -> new DocumentInfo(
                    doc.id(),
                    doc.filename(),
                    doc.content().length(),
                    doc.content().substring(0, Math.min(200, doc.content().length())) +
                        (doc.content().length() > 200 ? "..." : ""),
                    doc.createdAt()
                ))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

            return ResponseEntity.ok(new DocumentListResponse(
                documents,
                documentStore.get().getDocumentCount(),
                null
            ));
        } catch (Exception e) {
            log.error("Failed to list documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DocumentListResponse(List.of(), 0, "Failed to retrieve documents"));
        }
    }

    /**
     * POST /api/rag/documents
     * Upload a new document to the vector store
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload Document",
        description = "Upload a text document to be indexed in the RAG vector store. Supports .txt, .md, .json, .csv files."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Document uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file format"),
        @ApiResponse(responseCode = "503", description = "RAG service not available")
    })
    public ResponseEntity<UploadResponse> uploadDocument(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional description for the document")
            @RequestParam(value = "description", required = false) String description) {

        if (documentStore.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new UploadResponse(null, false, "RAG service is not enabled"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new UploadResponse(null, false, "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (!isValidFileType(filename)) {
            return ResponseEntity.badRequest()
                .body(new UploadResponse(null, false,
                    "Invalid file type. Supported: .txt, .md, .json, .csv, .log"));
        }

        try {
            // Read file content
            String content = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

            if (content.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(new UploadResponse(null, false, "File content is empty"));
            }

            // Add description as metadata if provided
            if (description != null && !description.isBlank()) {
                content = "Description: " + description + "\n\n" + content;
            }

            // Generate a simple embedding (in production, use AI model for real embeddings)
            float[] embedding = generateSimpleEmbedding(content);

            // Store document
            String docId = documentStore.get().addDocument(null, content, filename, embedding);

            log.info("Document uploaded successfully: {} ({})", filename, docId);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadResponse(docId, true, "Document uploaded successfully"));

        } catch (Exception e) {
            log.error("Failed to upload document: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UploadResponse(null, false, "Failed to upload document: " + e.getMessage()));
        }
    }

    /**
     * POST /api/rag/documents/text
     * Upload text content directly (no file)
     */
    @PostMapping("/documents/text")
    @Operation(
        summary = "Upload Text Content",
        description = "Upload raw text content to be indexed in the RAG vector store"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Content uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid content"),
        @ApiResponse(responseCode = "503", description = "RAG service not available")
    })
    public ResponseEntity<UploadResponse> uploadTextContent(@RequestBody TextUploadRequest request) {
        if (documentStore.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new UploadResponse(null, false, "RAG service is not enabled"));
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest()
                .body(new UploadResponse(null, false, "Content cannot be empty"));
        }

        try {
            String content = request.getContent();
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                content = "Title: " + request.getTitle() + "\n\n" + content;
            }

            float[] embedding = generateSimpleEmbedding(content);
            String filename = request.getTitle() != null ? request.getTitle() + ".txt" : "text-content.txt";
            String docId = documentStore.get().addDocument(null, content, filename, embedding);

            log.info("Text content uploaded successfully: {}", docId);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadResponse(docId, true, "Content uploaded successfully"));

        } catch (Exception e) {
            log.error("Failed to upload text content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UploadResponse(null, false, "Failed to upload content: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/rag/documents/{id}
     * Delete a document from the vector store
     */
    @DeleteMapping("/documents/{id}")
    @Operation(
        summary = "Delete Document",
        description = "Remove a document from the RAG vector store"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found"),
        @ApiResponse(responseCode = "503", description = "RAG service not available")
    })
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @Parameter(description = "Document ID", required = true)
            @PathVariable String id) {

        if (documentStore.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("success", false, "message", "RAG service is not enabled"));
        }

        try {
            boolean removed = documentStore.get().removeDocument(id);
            if (removed) {
                log.info("Document deleted: {}", id);
                return ResponseEntity.ok(Map.of("success", true, "message", "Document deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Document not found"));
            }
        } catch (Exception e) {
            log.error("Failed to delete document: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to delete document"));
        }
    }

    /**
     * DELETE /api/rag/documents
     * Clear all documents from the vector store
     */
    @DeleteMapping("/documents")
    @Operation(
        summary = "Clear All Documents",
        description = "Remove all documents from the RAG vector store"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All documents cleared"),
        @ApiResponse(responseCode = "503", description = "RAG service not available")
    })
    public ResponseEntity<Map<String, Object>> clearAllDocuments() {
        if (documentStore.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("success", false, "message", "RAG service is not enabled"));
        }

        try {
            int count = documentStore.get().getDocumentCount();
            documentStore.get().clear();
            log.info("All {} documents cleared from vector store", count);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All documents cleared",
                "deletedCount", count
            ));
        } catch (Exception e) {
            log.error("Failed to clear documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to clear documents"));
        }
    }

    /**
     * GET /api/rag/stats
     * Get vector store statistics
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Get RAG Statistics",
        description = "Returns statistics about the RAG vector store"
    )
    public ResponseEntity<Map<String, Object>> getStats() {
        if (documentStore.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("enabled", false, "message", "RAG service is not enabled"));
        }

        try {
            var docs = documentStore.get().getAllDocuments();
            long totalChars = docs.stream().mapToLong(d -> d.content().length()).sum();

            return ResponseEntity.ok(Map.of(
                "enabled", true,
                "documentCount", documentStore.get().getDocumentCount(),
                "totalCharacters", totalChars,
                "vectorStoreType", "SimpleDocumentStore (In-Memory)",
                "embeddingDimension", 384,
                "similarityMetric", "Cosine Similarity"
            ));
        } catch (Exception e) {
            log.error("Failed to get RAG stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("enabled", true, "error", e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    private boolean isValidFileType(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") ||
               lower.endsWith(".json") || lower.endsWith(".csv") ||
               lower.endsWith(".log") || lower.endsWith(".xml");
    }

    /**
     * Generate a simple embedding for demonstration.
     * In production, use an AI model (e.g., Ollama embeddings) for real embeddings.
     */
    private float[] generateSimpleEmbedding(String content) {
        // Simple hash-based embedding for demonstration
        // In production, use: ollamaClient.embeddings(content)
        float[] embedding = new float[384];
        int hash = content.hashCode();
        Random random = new Random(hash);
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = random.nextFloat() * 2 - 1; // -1 to 1
        }
        // Normalize
        float norm = 0;
        for (float v : embedding) norm += v * v;
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }
        return embedding;
    }

    // ==================== DTOs ====================

    @Data
    public static class DocumentListResponse {
        private final List<DocumentInfo> documents;
        private final int totalCount;
        private final String error;
    }

    @Data
    public static class DocumentInfo {
        private final String id;
        private final String filename;
        private final int contentLength;
        private final String preview;
        private final Instant createdAt;
    }

    @Data
    public static class UploadResponse {
        private final String documentId;
        private final boolean success;
        private final String message;
    }

    @Data
    public static class TextUploadRequest {
        private String title;
        private String content;
    }
}

