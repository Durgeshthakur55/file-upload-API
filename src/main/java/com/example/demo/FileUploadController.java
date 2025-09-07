package com.example.demo; // Change to your actual package name

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    // Single file upload
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String storedFileName = fileStorageService.storeFile(file);
            return ResponseEntity.ok("File uploaded successfully: " + storedFileName);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not upload file: " + e.getMessage());
        }
    }

    // Multiple files upload
    @PostMapping("/upload-multiple")
    public ResponseEntity<String> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        StringBuilder result = new StringBuilder();
        for (MultipartFile file : files) {
            try {
                String storedFileName = fileStorageService.storeFile(file);
                result.append("Uploaded: ").append(storedFileName).append("<br/>");
            } catch (RuntimeException e) {
                result.append("Failed: ").append(file.getOriginalFilename()).append(" (")
                      .append(e.getMessage()).append(")<br/>");
            }
        }
        return ResponseEntity.ok(result.toString());
    }

    // List all files with metadata (filename and size)
    @GetMapping
    public List<FileMetadata> listFiles() {
        return fileStorageService.loadAllFiles();
    }

    // Delete file by filename
    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        try {
            fileStorageService.deleteFile(filename);
            return ResponseEntity.ok("Deleted file: " + filename);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not delete file: " + e.getMessage());
        }
    }

    // Serve file inline for preview (image/PDF/etc.) or download
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = fileStorageService.getFileStorageLocation().resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                // Detect MIME type using probeContentType
                String mimeType = Files.probeContentType(filePath);
                if (mimeType == null) {
                    mimeType = "application/octet-stream"; // Default if unknown
                }
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType(mimeType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException | RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DTO class for file metadata
    public static class FileMetadata {
        private String filename;
        private long size;

        public FileMetadata(String filename, long size) {
            this.filename = filename;
            this.size = size;
        }

        public String getFilename() { return filename; }
        public long getSize() { return size; }
    }
}
