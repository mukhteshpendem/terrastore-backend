package com.mukhtesh.storage.repository;

import com.mukhtesh.storage.model.FileMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileMetaRepository extends JpaRepository<FileMeta, Long> {
    List<FileMeta> findByUserId(String userId);
    List<FileMeta> findByUserIdAndFileNameContainingIgnoreCase(String userId, String keyword);
}

