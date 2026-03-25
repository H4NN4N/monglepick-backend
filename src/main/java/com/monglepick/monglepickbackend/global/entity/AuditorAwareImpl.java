package com.monglepick.monglepickbackend.global.entity;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring Data JPA Auditing용 현재 사용자(Auditor) 제공자.
 *
 * <p>SecurityContext에서 인증 정보를 읽어 현재 요청을 수행하는 사용자 ID를 반환한다.</p>
 * <ul>
 *   <li>JWT 인증 → {@code authentication.getName()} (userId)</li>
 *   <li>미인증 / 시스템 배치 → {@code "SYSTEM"}</li>
 * </ul>
 *
 * <p>{@link BaseAuditEntity}의 {@code created_by}, {@code updated_by} 필드에 자동 주입된다.</p>
 */
@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    /**
     * 현재 요청의 사용자 ID를 반환한다.
     *
     * @return 현재 사용자 ID (인증되지 않은 경우 "SYSTEM")
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나, 미인증 상태이거나, 익명 사용자인 경우 → SYSTEM
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of("SYSTEM");
        }

        return Optional.of(auth.getName());
    }
}
