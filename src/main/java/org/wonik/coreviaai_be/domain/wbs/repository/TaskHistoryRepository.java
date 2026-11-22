package org.wonik.coreviaai_be.domain.wbs.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskHistory;

/**
 * 업무 이력 저장소. 추가만 하는 기록이라 저장·조회 메서드만 연다(삭제·수정 메서드 없음, JpaRepository를 상속하지 않음).
 */
public interface TaskHistoryRepository extends Repository<TaskHistory, Long> {

	TaskHistory save(TaskHistory history);

	<S extends TaskHistory> List<S> saveAll(Iterable<S> histories);

	/** 업무 이력(커서 방식, id 오름차순 = 시간순). 호출자가 size + 1개를 요청한다. idx_task_histories_task_id_id 사용. */
	@Query("""
		select h from TaskHistory h
		where h.taskId = :taskId
		  and (:cursorId is null or h.id > :cursorId)
		order by h.id asc
		""")
	List<TaskHistory> findPageByTaskId(
		@Param("taskId") Long taskId,
		@Param("cursorId") Long cursorId,
		Pageable pageable
	);

	/** 테스트·운영 확인용: 업무의 전체 이력(시간순). */
	List<TaskHistory> findAllByTaskIdOrderByIdAsc(Long taskId);
}
