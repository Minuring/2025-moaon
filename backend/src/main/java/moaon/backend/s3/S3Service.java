package moaon.backend.s3;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Duration PRESIGN_EXPIRATION = Duration.ofSeconds(30);

    private final S3Presigner s3Presigner;

    @Value("${s3.bucket}")
    private String bucket;

    @Transactional(readOnly = true)
    public List<S3UrlResponse> getPutS3Url(List<String> fileNames) {
        return fileNames.stream()
                .map(fileName -> {
                    String key = "moaon/projects/" + UUID.randomUUID() + "-" + fileName;
                    PresignedPutObjectRequest presignedRequest = presign(key);
                    return new S3UrlResponse(presignedRequest.url().toExternalForm(), fileName, key);
                }).toList();
    }

    private PresignedPutObjectRequest presign(String key) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_EXPIRATION)
                .putObjectRequest(objectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest);
    }
}
