package com.monglepick.monglepickbackend.domain.support.repository;

import com.monglepick.monglepickbackend.domain.support.entity.SupportFaqFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * FAQ 피드백 JPA 레포지토리.
 *
 * <p>{@link SupportFaqFeedback} 엔티티에 대한 데이터 접근 계층.
 * 중복 피드백 제출 방지를 위한 사전 조회 메서드를 제공한다.</p>
 */
public interface SupportFaqFeedbackRepository extends JpaRepository<SupportFaqFeedback, Long> {

    /**
     * 특정 FAQ에 대한 특정 사용자의 피드백을 조회한다.
     *
     * <p>피드백 제출 전 중복 여부를 확인하는 데 사용한다.
     * (faq_id, user_id) 복합 유니크 제약이 DB에 걸려 있으므로
     * 서비스 레이어에서 이 메서드로 사전 검사 후 중복이면
     * {@code ErrorCode.FAQ_FEEDBACK_DUPLICATE} 예외를 던진다.</p>
     *
     * <p>Spring Data JPA 메서드 이름 규칙:
     * {@code faq.faqId} 경로 → {@code findByFaq_FaqId},
     * userId 필드 → {@code AndUserId}</p>
     *
     * <pre>{@code
     * // 사용 예 (서비스 레이어)
     * feedbackRepository.findByFaq_FaqIdAndUserId(faqId, userId)
     *     .ifPresent(f -> { throw new BusinessException(ErrorCode.FAQ_FEEDBACK_DUPLICATE); });
     * }</pre>
     *
     * @param faqId  피드백 대상 FAQ ID
     * @param userId 피드백 제출 사용자 ID
     * @return 기존 피드백이 존재하면 Optional.of(feedback), 없으면 Optional.empty()
     */
    Optional<SupportFaqFeedback> findByFaq_FaqIdAndUserId(Long faqId, String userId);
}
