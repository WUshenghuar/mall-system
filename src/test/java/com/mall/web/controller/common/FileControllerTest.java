package com.mall.web.controller.common;

import com.mall.common.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileControllerTest {
    @Test
    void productImageRejectsNonImageBeforeStorageCall() {
        FileService fileService = mock(FileService.class);
        FileController controller = new FileController(fileService);
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "not image".getBytes());

        assertThatThrownBy(() -> controller.uploadProductImage(file))
                .hasMessage("商品图片必须是 5MB 以内的图片文件");
        verifyNoInteractions(fileService);
    }

    @Test
    void productImageReturnsStoredPathAndPreviewUrl() {
        FileService fileService = mock(FileService.class);
        when(fileService.upload(any())).thenReturn("mall-product/cover.jpg");
        when(fileService.getPresignedUrl("mall-product/cover.jpg")).thenReturn("http://localhost/cover.jpg");
        FileController controller = new FileController(fileService);

        var result = controller.uploadProductImage(new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1}));

        assertThat(result.getData()).containsEntry("path", "mall-product/cover.jpg")
                .containsEntry("url", "http://localhost/cover.jpg");
        verify(fileService).upload(any());
    }
}
