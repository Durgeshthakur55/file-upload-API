package com.example.demo;  // replace with your package

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "pdf");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/png", "image/jpg", "image/jpeg", "application/pdf");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("./uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory!", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds limit of 5MB");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        if (originalFileName.contains("..")) {
            throw new RuntimeException("Invalid path sequence in filename: " + originalFileName);
        }

        String fileExt = getFileExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(fileExt.toLowerCase())) {
            throw new RuntimeException("File type '" + fileExt + "' not allowed.");
        }

        String mimeType = file.getContentType();
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new RuntimeException("MIME type '" + mimeType + "' not allowed.");
        }

        String uniqueFileName = generateUniqueFileName(originalFileName);

        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + uniqueFileName + ". Please try again!", ex);
        }
    }

    public List<FileUploadController.FileMetadata> loadAllFiles() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileStorageLocation)) {
            List<FileUploadController.FileMetadata> files = new ArrayList<>();
            for (Path path : stream) {
                File file = path.toFile();
                if (file.isFile()) {
                    files.add(new FileUploadController.FileMetadata(file.getName(), file.length()));
                }
            }
            return files;
        } catch (IOException e) {
            throw new RuntimeException("Could not list files", e);
        }
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            File fileToDelete = filePath.toFile();
            if (!fileToDelete.exists()) {
                throw new RuntimeException("File not found: " + filename);
            }
            if (!fileToDelete.delete()) {
                throw new RuntimeException("Failed to delete file: " + filename);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not delete file: " + e.getMessage(), e);
        }
    }

    public Path getFileStorageLocation() {
        return this.fileStorageLocation;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return fileName.substring(dotIndex + 1);
    }

    private String generateUniqueFileName(String originalName) {
        String ext = getFileExtension(originalName);
        String uuid = UUID.randomUUID().toString();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return uuid + "_" + timestamp + "." + ext;
    }
}
