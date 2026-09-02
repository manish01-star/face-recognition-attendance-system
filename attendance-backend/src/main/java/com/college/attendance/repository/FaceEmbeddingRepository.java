package com.college.attendance.repository;

import com.college.attendance.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, Long> {

        Optional<FaceEmbedding> findByUserId(Long userId);

        boolean existsByUserId(Long userId);

        List<FaceEmbedding> findAllByStatus(String status);

        Optional<FaceEmbedding> findByUserIdAndStatus(
                        Long userId,
                        String status);
}