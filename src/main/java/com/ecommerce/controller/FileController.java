package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.service.interfaces.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
// Using fully qualified name for Swagger ApiResponse to avoid collision with DTO ApiResponse
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File", description = "API quản lý tệp tin")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Operation(summary = "Tải lên tệp tin", description = "Tải lên một tệp tin vào hệ thống", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tải lên tệp tin thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tệp tin không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lưu tệp tin",
                content = @Content)
    })
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @Parameter(description = "Tệp tin cần tải lên", required = true) @RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/")
                .path(fileName)
                .toUriString();

        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully",fileDownloadUri));
    }

    @Operation(summary = "Tải lên nhiều tệp tin", description = "Tải lên nhiều tệp tin vào hệ thống", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tải lên các tệp tin thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tệp tin không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lưu tệp tin",
                content = @Content)
    })
    @PostMapping("/uploadMultiple")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<List<String>>> uploadMultipleFiles(
            @Parameter(description = "Danh sách tệp tin cần tải lên", required = true) @RequestParam("files") MultipartFile[] files) {
        List<String> fileDownloadUrls = Arrays.stream(files)
                .map(file -> {
                    String fileName = fileStorageService.storeFile(file);
                    return ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/files/")
                            .path(fileName)
                            .toUriString();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("All files uploaded successfully", fileDownloadUrls));
    }

    @Operation(summary = "Tải xuống tệp tin", description = "Tải xuống tệp tin từ hệ thống")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tải xuống tệp tin thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tệp tin",
                content = @Content)
    })
    @GetMapping("/{fileName:.+}")
    @RateLimit(authenticatedLimit = 100, anonymousLimit = 50, refreshPeriod = 60)
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Tên tệp tin cần tải xuống", required = true) @PathVariable String fileName, 
            HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}