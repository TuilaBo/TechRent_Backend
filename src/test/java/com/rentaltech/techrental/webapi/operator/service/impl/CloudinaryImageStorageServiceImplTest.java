package com.rentaltech.techrental.webapi.operator.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageStorageServiceImplTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;
    @Mock
    private MultipartFile multipartFile;

    private CloudinaryImageStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CloudinaryImageStorageServiceImpl(cloudinary);
    }

    @Test
    void uploadDeviceModelImageUsesBrandFolderAndSlugifiedName() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn("bytes".getBytes());
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://cdn.example/test.png"));

        String url = service.uploadDeviceModelImage(multipartFile, 5L, "Galaxy S24 Ultra!");

        assertThat(url).isEqualTo("https://cdn.example/test.png");
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        Map<String, Object> options = captor.getValue();
        assertThat(options.get("folder")).isEqualTo("techrental/device-models/brand_5");
        assertThat(options.get("public_id").toString()).startsWith("galaxy-s24-ultra_");
    }

    @Test
    void uploadPolicyFileSetsRawResourceType() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn("bytes".getBytes());
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://cdn.example/test.png"));
        String url = service.uploadPolicyFile(multipartFile, null, "Policy #1.pdf");

        assertThat(url).isEqualTo("https://cdn.example/test.png");
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue().get("resource_type")).isEqualTo("raw");
    }

    @Test
    void uploadQcAccessorySnapshotRejectsEmptyFile() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> service.uploadQcAccessorySnapshot(multipartFile, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tệp rỗng");
        verify(uploader, never()).upload(any(byte[].class), any(Map.class));
    }
}
