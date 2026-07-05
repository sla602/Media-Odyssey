package com.mo.mediaodyssey.shared.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Service
public class ObjectStorageService {

    /**
     * Service to access S3 compatible object storage solutions. Not limited to one
     * object storage provider. Any S3 compatible object storage provider can be
     * used.
     * 
     * For Media Odyssey, we have chosen Backblaze B2. Backblaze does not require
     * payment information and provides 10GB for free. However, without payment,
     * their buckets are private by default.
     * However, using Cloudflare Workers, public requests to the Cloudflare Worker
     * can sign requests using our Application Key to our private Backblaze bucket
     * using the provided sample/guide:
     * https://www.backblaze.com/docs/cloud-storage-deliver-private-backblaze-b2-content-through-cloudflare-cdn
     * https://github.com/backblaze-b2-samples/cloudflare-b2/tree/main
     * 
     * Furthermore, using Backblaze and Cloudflare together results in free and
     * unlimited egress.
     */

    @Autowired
    private S3Client s3Client;

    @Value("${storage.bucket}")
    private String bucket;

    @Value("${spring.application.name}")
    private String appName;

    /**
     * Uploads a file to the object storage.
     *
     * @param file The file to upload.
     * @return The key of the uploaded file.
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was provided for upload.");
        }

        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        String key = appName.replaceAll("\\s+", "_") + "-" + UUID.randomUUID() + "-" + original.replaceAll("\\s+", "_");

        try (InputStream is = file.getInputStream()) {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putReq, RequestBody.fromInputStream(is, file.getSize()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }

        return key;
    }

    public void deleteFile(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key must be provided");
        }

        var deleteRequest = software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);
    }
}
