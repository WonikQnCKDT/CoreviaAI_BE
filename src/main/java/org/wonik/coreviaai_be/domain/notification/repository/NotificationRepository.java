package org.wonik.coreviaai_be.domain.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.notification.entity.Notification;
import org.wonik.coreviaai_be.domain.notification.entity.NotificationTargetType;
import org.wonik.coreviaai_be.domain.notification.entity.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

	long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

	/**
	 * 수신자 목록(최신순, id 내림차순). unreadOnly면 미확인만. 호출자가 size + 1개를 요청한다.
	 */
	@Query("""
		select n from Notification n
		where n.recipientUserId = :recipientUserId
		  and (:cursorId is null or n.id < :cursorId)
		  and (:unreadOnly = false or n.readAt is null)
		order by n.id desc
		""")
	List<Notification> findPageForRecipient(
		@Param("recipientUserId") Long recipientUserId,
		@Param("cursorId") Long cursorId,
		@Param("unreadOnly") boolean unreadOnly,
		Pageable pageable
	);

	boolean existsByRecipientUserIdAndNotificationTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqual(
		Long recipientUserId,
		NotificationType notificationType,
		NotificationTargetType targetType,
		Long targetId,
		Instant createdAtFrom
	);
}
