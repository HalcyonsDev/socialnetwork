package com.halcyon.userservice.service;

import com.halcyon.userservice.exception.StorageInitializationException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path rootLocation;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png");

    public FileStorageService(
            @Value("${file.storage.location}") String storageLocation
    ) {
        this.rootLocation = Paths.get(storageLocation);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException ex) {
            throw new StorageInitializationException("Could not initialize storage.", ex);
        }
    }

    public String upload(MultipartFile file) {
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new StorageInitializationException("Image type should be jpeg/png");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String filename = UUID.randomUUID() + "-" + originalFilename;

        if (file.isEmpty()) {
            throw new StorageInitializationException("File is empty.");
        }

        Path destinationFile = rootLocation.resolve(filename).normalize().toAbsolutePath();

        if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())) {
            throw new StorageInitializationException("Can't store file outside current directory.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new StorageInitializationException("Failed to store file.", ex);
        }

        return destinationFile.toString();
    }

    public File getFileByPath(String filepath) {
        return Paths.get(filepath).toFile();
    }
}
