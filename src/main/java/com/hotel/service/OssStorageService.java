package com.hotel.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.hotel.config.OssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OssStorageService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private final OssProperties ossProperties;

    public String uploadRoomTypeImage(MultipartFile file) {
        validateConfig();
        validateFile(file);

        String objectKey = buildObjectKey(file.getOriginalFilename());
        OSS client = new OSSClientBuilder().build(
                normalizeEndpoint(ossProperties.getEndpoint()),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            client.putObject(ossProperties.getBucketName(), objectKey, inputStream, metadata);
            return buildPublicUrl(objectKey);
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取上传文件失败", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("上传到OSS失败", ex);
        } finally {
            client.shutdown();
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(ossProperties.getEndpoint())
                || !StringUtils.hasText(ossProperties.getBucketName())
                || !StringUtils.hasText(ossProperties.getAccessKeyId())
                || !StringUtils.hasText(ossProperties.getAccessKeySecret())) {
            throw new IllegalStateException("OSS配置不完整，请检查 endpoint/bucket/access-key");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过10MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片文件上传");
        }
    }

    private String buildObjectKey(String originalFilename) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String extension = extractExtension(originalFilename);
        return "room-types/" + datePart + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ".jpg";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return ".jpg";
        }
        String ext = fileName.substring(lastDot).toLowerCase(Locale.ROOT);
        if (ext.length() > 10) {
            return ".jpg";
        }
        return ext;
    }

    private String buildPublicUrl(String objectKey) {
        if (StringUtils.hasText(ossProperties.getPublicBaseUrl())) {
            String base = ossProperties.getPublicBaseUrl().replaceAll("/+$", "");
            return base + "/" + objectKey;
        }
        String endpoint = ossProperties.getEndpoint().replaceFirst("^https?://", "");
        return "https://" + ossProperties.getBucketName() + "." + endpoint + "/" + objectKey;
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return "https://" + endpoint;
    }
}

