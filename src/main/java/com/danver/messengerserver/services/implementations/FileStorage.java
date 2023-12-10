package com.danver.messengerserver.services.implementations;


import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.services.interfaces.StorageService;
import com.danver.messengerserver.utils.FileStorageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@Qualifier("fileStorage")
public class FileStorage implements StorageService {

    private final Environment env;
    private final Path rootLocation;

    @Autowired
    public FileStorage(Environment env) {
        this.env = env;
        rootLocation = Paths.get(Objects.requireNonNull(this.env.getProperty("filebase.root.folder.path")))
                .toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) throws StorageException {
        Path destinationFile = this.rootLocation.resolve(
                        Paths.get(Objects.requireNonNull(file.getOriginalFilename()))
                )
                .toAbsolutePath()
                .normalize();
        try (InputStream stream = file.getInputStream()) {
            Files.copy(
                    stream,
                    destinationFile,
                    StandardCopyOption.REPLACE_EXISTING
            );
            return null;
        } catch (IOException e) {
            throw new StorageException("Couldn't upload file " + file.getOriginalFilename() + " to the server");
        }
    }

    @Override
    public String store(MultipartFile file, FileStorageOptions options) throws StorageException {
        String filename = file.getOriginalFilename();
        Path destinationFile;
        if (options.getFileName() != null) {
            filename = options.getFileName();
        } else {
            // Add some randomness to filename
            filename = UUID.randomUUID().toString() + '_' + filename;
        }
        ;
        if (options.getPath() != null) {
            destinationFile = this.rootLocation.resolve(Paths.get(options.getPath() + '/' + filename))
                    .toAbsolutePath()
                    .normalize();
        } else {
            destinationFile = this.rootLocation.resolve(
                            Paths.get(Objects.requireNonNull(filename)))
                    .toAbsolutePath()
                    .normalize();
        }
        try (InputStream stream = file.getInputStream()) {
            File directories = new File(destinationFile.getParent().toString());
            if (!directories.exists()) {
                Files.createDirectories(destinationFile.getParent());
            }
            Files.copy(
                    stream,
                    destinationFile,
                    StandardCopyOption.REPLACE_EXISTING
            );
            return destinationFile.toString();
        } catch (IOException e) {
            throw new StorageException("Couldn't upload file " + file.getOriginalFilename() + " to the server");
        }
    }

    @Override
    public void load(String filename) {

    }

    @Override
    public String getRootPath() {
        return env.getProperty("filebase.root.folder.path");
    }
}
