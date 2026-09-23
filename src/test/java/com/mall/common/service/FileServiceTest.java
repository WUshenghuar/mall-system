package com.mall.common.service;

import com.mall.common.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {
    @Test
    void uploadStripsDirectoriesFromClientFilename() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any())).thenReturn(true);
        MinioConfig config = new MinioConfig();
        config.setBucket("mall-product");
        FileService service = new FileService(client, config);

        service.upload(new MockMultipartFile("file", "../../payload.jpg", "image/jpeg", new byte[]{1}));

        ArgumentCaptor<PutObjectArgs> args = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(args.capture());
        assertThat(args.getValue().object()).doesNotContain("..", "/", "\\").endsWith("payload.jpg");
    }

    @Test
    void uploadReplacesTraversalOnlyFilename() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any())).thenReturn(true);
        MinioConfig config = new MinioConfig();
        config.setBucket("mall-product");

        String path = new FileService(client, config).upload(
                new MockMultipartFile("file", "..", "image/jpeg", new byte[]{1}));

        assertThat(path).doesNotContain("..").endsWith("-upload");
    }

    @Test
    void deleteRejectsPathsOutsideConfiguredBucket() {
        MinioConfig config = new MinioConfig();
        config.setBucket("mall-product");
        FileService service = new FileService(mock(MinioClient.class), config);

        assertThatThrownBy(() -> service.delete("other-bucket/secret.jpg"))
                .hasMessageContaining("文件路径不合法");
    }

    @Test
    void deleteReportsStorageFailure() throws Exception {
        MinioClient client = mock(MinioClient.class);
        doThrow(new RuntimeException("offline")).when(client).removeObject(any());
        MinioConfig config = new MinioConfig();
        config.setBucket("mall-product");

        assertThatThrownBy(() -> new FileService(client, config).delete("mall-product/cover.jpg"))
                .hasMessageContaining("文件删除失败");
    }
}
