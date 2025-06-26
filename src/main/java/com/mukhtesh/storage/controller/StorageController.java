package com.mukhtesh.storage.controller;

import com.mukhtesh.storage.model.FileMeta;
import com.mukhtesh.storage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class StorageController {

    private final StorageService service;

    public StorageController(StorageService service) {
        this.service = service;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestPart MultipartFile file,
                                         @AuthenticationPrincipal Jwt jwt) throws IOException {
        System.out.println("Filename: " + file.getOriginalFilename());
        System.out.println("MIME Type: " + file.getContentType());

        List<String> allowed = List.of(
                "image/png", "image/jpeg", "image/gif",
                "video/mp4", "audio/mpeg", "application/pdf"
        );

        if (!allowed.contains(file.getContentType())) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("❌ Unsupported file type: " + file.getContentType());
        }

        service.upload(file, jwt.getSubject());
        return ResponseEntity.ok("✅ File uploaded");
    }


    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String key) throws IOException {
        return service.downloadFileResponse(key);  // 🔥 Delegates to service
    }

    @GetMapping("/files")
    public List<FileMeta> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(jwt.getSubject());
    }

    @GetMapping("/search")
    public List<FileMeta> search(@RequestParam String q, @AuthenticationPrincipal Jwt jwt) {
        return service.search(jwt.getSubject(), q);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        try {
            service.delete(jwt.getSubject(), id);
            return ResponseEntity.ok("✅ File deleted");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ " + e.getMessage());
        }
    }


}

