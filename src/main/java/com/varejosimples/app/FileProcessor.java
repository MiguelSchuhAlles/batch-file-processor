package com.varejosimples.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileProcessor {
    private static final List<String> COMPANY_BUCKETS = new ArrayList<>(Arrays.asList(
            "varejosimples-testcompany1",
            "varejosimples-testcompany2"));

    private static final String LOCAL_FILE_PROCESSING_FOLDER = "/tmp/processing";

    private final S3FileManager s3FileManager = new S3FileManager();

    public void processFiles() {
        for (String companyBucket : COMPANY_BUCKETS) {
            log.info("Starting processing of files in bucket {}", companyBucket);
            s3FileManager.listPendingFiles(companyBucket)
                    .forEach(fileKey -> {
                        log.info("Found file: {} at bucket {}", fileKey, companyBucket);

                        downloadFile(companyBucket, fileKey);
                        processFile(fileKey);
                        archiveFile(companyBucket, fileKey);
                    });
        }
    }

    private void downloadFile(String companyBucket, String fileKey) {
        s3FileManager.downloadFile(companyBucket, fileKey, LOCAL_FILE_PROCESSING_FOLDER);
    }

    // placeholder method
    private void processFile(String fileKey) {
        String filePath = LOCAL_FILE_PROCESSING_FOLDER + fileKey;
        log.info("Processing file {}", filePath);
    }

    private void archiveFile(String companyBucket, String fileKey) {
        s3FileManager.archiveProcessedFile(companyBucket, fileKey);
    }
}
