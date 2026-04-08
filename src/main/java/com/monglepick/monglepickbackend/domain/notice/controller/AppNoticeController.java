package com.monglepick.monglepickbackend.domain.notice.controller;

import com.monglepick.monglepickbackend.admin.dto.AdminSupportDto.NoticeResponse;
import com.monglepick.monglepickbackend.admin.service.AdminSupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 앱 메인 공지 사용자 조회 컨트롤러 (비로그인 허용).
 *
 * <p>2026-04-08 개편: AppNotice 엔티티 폐기로 본 컨트롤러는 SupportNotice의
 * displayType IN (BANNER/POPUP/MODAL) 레코드를 조회하도록 변경되었다.
 * 공개 엔드포인트 경로 {@code GET /api/v1/notices} 는 하위 호환 유지.</p>
 *
 * <p>활성 + 기간 조건을 만족하는 공지만 반환한다:
 * {@code is_active=true AND (start_at IS NULL OR start_at <= NOW())
 *        AND (end_at IS NULL OR end_at >= NOW())}</p>
 */
@Tag(name = "공지사항", description = "앱 메인 BANNER/POPUP/MODAL 공지 조회 (비로그인 허용)")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@Slf4j
public class AppNoticeController {

    /** 2026-04-08: 구 AdminAppNoticeService → AdminSupportService(통합 서비스)로 전환 */
    private final AdminSupportService adminSupportService;

    /**
     * 현재 노출 중인 앱 메인 공지 목록 조회.
     *
     * @param type BANNER/POPUP/MODAL 필터 (생략 시 앱 메인 전체)
     * @return 노출 중 공지 목록 (priority DESC, createdAt DESC)
     */
    @Operation(
            summary = "노출 중 공지 조회",
            description = "현재 시각 기준 is_active=true AND 시작일~종료일 범위 내인 "
                    + "BANNER/POPUP/MODAL 공지만 반환 (LIST_ONLY 공지는 제외)"
    )
    @SecurityRequirement(name = "")
    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getActiveNotices(
            @Parameter(description = "공지 종류 필터 (BANNER/POPUP/MODAL, 생략 시 전체)")
            @RequestParam(required = false) String type
    ) {
        log.debug("[AppNoticeController] 노출 중 공지 조회 — type={}", type);
        return ResponseEntity.ok(adminSupportService.getActiveAppNotices(type));
    }
}
