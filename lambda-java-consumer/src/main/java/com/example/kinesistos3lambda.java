package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class kinesistos3lambda implements RequestHandler<KinesisEvent, String> {

    private final S3Client s3 = S3Client.create(); // Uses Lambda IAM Role
    private final String bucketName = System.getenv("BUCKET_NAME"); // Use env variable
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String handleRequest(KinesisEvent event, Context context) {
        for (KinesisEvent.KinesisEventRecord record : event.getRecords()) {
            try {
                String jsonString = new String(record.getKinesis().getData().array(), StandardCharsets.UTF_8);
                JsonNode json = mapper.readTree(jsonString);

                String fileName = "event-" + UUID.randomUUID() + ".json";
                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType("application/json")
                        .build();

                s3.putObject(putReq, software.amazon.awssdk.core.sync.RequestBody.fromString(json.toString()));
                context.getLogger().log("Uploaded: " + fileName);

            } catch (Exception e) {
                context.getLogger().log("Error: " + e.getMessage());
            }
        }

        return "Processed " + event.getRecords().size() + " records.";
    }
}
