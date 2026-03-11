package com.amine.geminiservice.repository;


import com.amine.geminiservice.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    /**
     * Find similar embeddings using cosine distance
     * Lower distance = more similar
     */
    @Query(value = "SELECT *, (embedding <=> CAST(:queryEmbedding AS vector)) AS distance " +
            "FROM document_embeddings " +
            "WHERE user_id = :userId " +
            "ORDER BY embedding <=> CAST(:queryEmbedding AS vector) " +
            "LIMIT :limit",
            nativeQuery = true)
    List<DocumentEmbedding> findSimilar(
            @Param("userId") String userId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit
    );

    List<DocumentEmbedding> findByUserId(String userId);

    void deleteByDocumentId(String documentId);
}