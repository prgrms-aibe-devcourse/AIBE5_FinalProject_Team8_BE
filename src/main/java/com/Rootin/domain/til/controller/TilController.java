// TIL 컨트롤러: TIL 작성·수정·삭제·임시저장 REST API 처리 (작성·임시저장은 multipart/form-data로 이미지 업로드 지원)
package com.Rootin.domain.til.controller;

import com.Rootin.domain.til.dto.request.DraftSaveRequest;
import com.Rootin.domain.til.dto.request.TilCreateRequest;
import com.Rootin.domain.til.dto.request.TilUpdateRequest;
import com.Rootin.domain.til.dto.response.TilResponse;
import com.Rootin.domain.til.service.TilService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.jwt.JwtUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
public class TilController {

    private final TilService tilService;

    /**
     * TIL 작성 (POST /api/v1/tils)
     *
     * multipart/form-data 요청:
     *   - data  (application/json, required): TIL 본문 JSON
     *   - image (image/*, optional)         : 썸네일 이미지 파일 → S3 업로드 후 thumbnailUrl 저장
     *
     * 이미지 없이 JSON만 전송하는 경우에도 정상 동작합니다.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ApiResponse<TilResponse>> create(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestPart("data") @Valid TilCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile thumbnailImage
    ) {
        TilResponse response = tilService.create(userDetails.getUserId(), request, thumbnailImage);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{tilId}")
    public ResponseEntity<ApiResponse<TilResponse>> findById(
            @PathVariable Long tilId,
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(tilService.findById(tilId, userDetails.getUserId())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<TilResponse>>> findMyTils(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(required = false) Long potId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                tilService.findMyTils(userDetails.getUserId(), potId, page, size, sort, keyword, tag)));
    }

    @PutMapping(value = "/{tilId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ApiResponse<TilResponse>> update(
            @PathVariable Long tilId,
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestPart("data") @Valid TilUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile thumbnailImage
    ) {
        return ResponseEntity.ok(ApiResponse.success(tilService.update(tilId, userDetails.getUserId(), request, thumbnailImage)));
    }

    @DeleteMapping("/{tilId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long tilId,
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        tilService.delete(tilId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/draft", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ApiResponse<TilResponse>> saveDraft(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestPart("data") @Valid DraftSaveRequest request,
            @RequestPart(value = "image", required = false) MultipartFile thumbnailImage
    ) {
        return ResponseEntity.ok(ApiResponse.success(tilService.saveDraft(userDetails.getUserId(), request, thumbnailImage)));
    }

    @GetMapping("/draft")
    public ResponseEntity<ApiResponse<TilResponse>> getDraft(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam Long potId
    ) {
        return ResponseEntity.ok(ApiResponse.success(tilService.getDraft(userDetails.getUserId(), potId)));
    }

    @DeleteMapping("/draft")
    public ResponseEntity<Void> deleteDraft(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam Long potId
    ) {
        tilService.deleteDraft(userDetails.getUserId(), potId);
        return ResponseEntity.noContent().build();
    }
}
