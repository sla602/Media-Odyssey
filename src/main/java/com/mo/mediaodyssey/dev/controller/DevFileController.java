package com.mo.mediaodyssey.dev.controller;

import com.mo.mediaodyssey.dev.dto.DevFileApiResponse;
import com.mo.mediaodyssey.dev.services.DevUserService;
import com.mo.mediaodyssey.shared.services.ObjectStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/dev/file")
public class DevFileController {

    @Autowired
    private ObjectStorageService objectStorageService;

    @Value("${dev.mode.enabled}")
    private String devMode;

    @Value("${storage.public-url}")
    private String publicUrl;

    @Autowired
    private DevUserService devUserService;

    /**
     * API endpoint to upload a file to object storage. For developmental use only.
     * Must be enabled via environment variable. Once enabled, access will be
     * permitted based on the environment variable setting.
     *
     * @param file Multipart file to be uploaded. Must be sent as form data with key
     *             "file".
     * @return URL of the uploaded file in body upon success
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DevFileApiResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (!devUserService.isDevModeEnabled()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(DevFileApiResponse.error("DEV_FILE_DISABLED",
                                "Developmental file operation endpoint is disabled."));
            } else {
                String key = objectStorageService.uploadFile(file);
                String url = publicUrl.replaceAll("/$", "") + "/" + key;

                return ResponseEntity
                        .ok(DevFileApiResponse.success("DEV_FILE_UPLOAD_SUCCESS", "File uploaded successfully.", key,
                                url));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(DevFileApiResponse.error("DEV_FILE_UPLOAD_ERROR",
                    "An error occurred while processing the request."));
        }
    }

    /**
     * API endpoint to delete a file from object storage. For developmental use
     * only. Must be enabled via environment variable. Once enabled, access will be
     * permitted based on the environment variable setting.
     *
     * @param key The key of the file to be deleted. Must be sent as a query
     *            parameter.
     * @return 200 OK with success message upon successful deletion, or appropriate
     *         error response.
     */
    @DeleteMapping(value = "/delete")
    public ResponseEntity<DevFileApiResponse> deleteFile(@RequestParam("key") String key) {
        try {
            if (!devUserService.isDevModeEnabled()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(DevFileApiResponse.error("DEV_FILE_DISABLED",
                                "Developmental file operation endpoint is disabled."));
            } else {
                objectStorageService.deleteFile(key);
                return ResponseEntity
                        .ok(DevFileApiResponse.success("DEV_FILE_DELETE_SUCCESS", "File deleted successfully.", key,
                                null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(DevFileApiResponse.error("DEV_FILE_DELETE_ERROR",
                    "An error occurred while deleting the file."));
        }
    }

    /**
     * API endpoint to get a redirect URL for a file in object storage. For
     * developmental use only. Must be enabled via environment variable. Once
     * enabled, access will be permitted based on the environment variable setting.
     *
     * @param key The key of the file for which to get the redirect URL.
     * @return 302 Found with Location header set to the file's public URL upon
     *         success, or appropriate error response.
     */
    @GetMapping(value = "/get")
    public ResponseEntity<DevFileApiResponse> getFileRedirect(@RequestParam("key") String key) {
        if (!devUserService.isDevModeEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(DevFileApiResponse.error("DEV_FILE_DISABLED",
                            "Developmental file operation endpoint is disabled."));
        } else if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(DevFileApiResponse.error("DEV_FILE_GET_ERROR", "Invalid file key provided."));
        } else {
            String redirectUrl = publicUrl.replaceAll("/$", "") + "/" + key.replaceAll("^/+", "");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        }
    }
}
