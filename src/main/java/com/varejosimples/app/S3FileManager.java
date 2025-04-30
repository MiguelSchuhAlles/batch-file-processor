package com.varejosimples.app;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
public class S3FileManager {
    private S3Client s3;

    public S3FileManager() {
        s3 = S3Client.create();
    }

    public Stream<String> listPendingFiles(String bucketName) {
        try {
            String prefix = "pending_processing/";

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response result = s3.listObjectsV2(request);

            return result.contents()
                    .stream()
                    .map(S3Object::key)
                    .filter(key -> !key.equals(prefix)); // filter out the result for "folder" itself;
        } catch (Exception e) {
            log.error("Error listing pending files for company bucket {}: ", bucketName, e);
            return Stream.empty();
        }
    }

    public void downloadFile(String bucketName, String s3Key, String localFolderPath) {
        try {
            String fileName = getLocalFileName(s3Key, localFolderPath);

            Path fullLocalPath = Paths.get(localFolderPath, fileName);
            Files.createDirectories(fullLocalPath.getParent()); // create parent dirs if needed

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(request);

            // Write file
            try (OutputStream out = new FileOutputStream(fullLocalPath.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = s3Object.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            log.info("File downloaded successfully to {}", fullLocalPath);

        } catch (Exception e) {
            log.error("Failed to download file {} from S3 bucket {}", s3Key, bucketName, e);
            throw new RuntimeException("Error downloading and saving file", e);
        }
    }

    public void archiveProcessedFile(String bucketName, String s3Key) {
        try {
            String fileName = Paths.get(s3Key).getFileName().toString();
            String sourceKey = "pending_processing/" + fileName;

            String yearMonth = java.time.LocalDate.now().toString().substring(0, 7); // format: yyyy-MM
            String destinationKey = String.format("processed/%s/%s", yearMonth, fileName);

            log.info("Archiving file from '{}' to '{}'", s3Key, destinationKey);

            copyToProcessedFolder(bucketName, sourceKey, destinationKey);

            deleteFromPendingProcessingFolder(bucketName, sourceKey);

        } catch (Exception e) {
            log.error("Failed to archive file {}: ", s3Key, e);
            throw new RuntimeException("Error archiving file", e);
        }
    }

    private void copyToProcessedFolder(String bucketName, String sourceKey, String newKey) {
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(newKey)
                .build();

        s3.copyObject(copyRequest);

        log.info("Copied file from {} to {}, at bucket {}", sourceKey, newKey, bucketName);
    }

    private void deleteFromPendingProcessingFolder(String bucketName, String sourceKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(sourceKey)
                .build();

        s3.deleteObject(deleteRequest);

        log.info("Deleted original file {}", sourceKey);
    }

    private String getLocalFileName(String s3Key, String localFolderPath) {
        String fileName = Paths.get(s3Key).getFileName().toString();
        return fileName;
    }

}
