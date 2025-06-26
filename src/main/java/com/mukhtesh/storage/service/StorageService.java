package com.mukhtesh.storage.service;

import com.mukhtesh.storage.model.FileMeta;
import com.mukhtesh.storage.repository.FileMetaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StorageService {

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    private final AmazonS3 s3;
    private final FileMetaRepository repo;

    public StorageService(AmazonS3 s3, FileMetaRepository repo) {
        this.s3 = s3;
        this.repo = repo;
    }

    public void upload(MultipartFile file, String userId) throws IOException {
        String key = userId + "/" + file.getOriginalFilename();

        s3.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), null)); // metadata optional

        FileMeta meta = new FileMeta();
        meta.setUserId(userId);
        meta.setFileName(file.getOriginalFilename());
        meta.setFileType(file.getContentType());
        meta.setS3Key(key);
        meta.setS3Url("https://" + bucket + ".s3.amazonaws.com/" + key);
        meta.setUploadedAt(LocalDateTime.now());

        repo.save(meta);
    }

    public ResponseEntity<byte[]> downloadFileResponse(String key) throws IOException {
        S3Object s3Object = s3.getObject(bucket, key);
        byte[] data = s3Object.getObjectContent().readAllBytes();

        String contentType = s3Object.getObjectMetadata().getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = Files.probeContentType(Paths.get(key));
            if (contentType == null) contentType = "application/octet-stream";
        }

        boolean previewable = contentType.startsWith("image/") || contentType.equals("application/pdf") || contentType.startsWith("video/") || contentType.startsWith("audio/");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(
                ContentDisposition.builder(previewable ? "inline" : "attachment")
                        .filename(key)
                        .build()
        );

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    public List<FileMeta> list(String userId) {
        return repo.findByUserId(userId);
    }

    public List<FileMeta> search(String userId, String keyword) {
        return repo.findByUserIdAndFileNameContainingIgnoreCase(userId, keyword);
    }

    public void delete(String userId, Long fileId) {
        FileMeta fileMeta = repo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!fileMeta.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized delete attempt");
        }

        s3.deleteObject(bucket, fileMeta.getS3Key());
        repo.deleteById(fileId);
    }

}
