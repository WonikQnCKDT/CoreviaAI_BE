package org.wonik.coreviaai_be.domain.wbs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneTask;
import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneLinkedTask;

public interface MilestoneTaskRepository extends JpaRepository<MilestoneTask, Long> {

	boolean existsByTaskId(Long taskId);

	List<MilestoneTask> findAllByMilestoneId(Long milestoneId);

	/**
	 * 여러 마일스톤의 연결 작업을 한 번에 읽는다(목록 조회 N+1 방지). 마일스톤 ID 순, 그 안에서 작업 ID 순.
	 */
	@Query("""
		select new org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneLinkedTask(
			mt.milestoneId, t.id, t.parentTaskId, t.name, t.status, t.endDate)
		from MilestoneTask mt join WbsTask t on t.id = mt.taskId
		where mt.milestoneId in :milestoneIds
		order by mt.milestoneId, t.id
		""")
	List<MilestoneLinkedTask> findLinkedTasks(@Param("milestoneIds") Collection<Long> milestoneIds);

	@Modifying(flushAutomatically = true)
	@Query("delete from MilestoneTask mt where mt.milestoneId = :milestoneId")
	int deleteAllByMilestoneId(@Param("milestoneId") Long milestoneId);

	@Modifying(flushAutomatically = true)
	@Query("delete from MilestoneTask mt where mt.milestoneId = :milestoneId and mt.taskId in :taskIds")
	int deleteLinks(@Param("milestoneId") Long milestoneId, @Param("taskIds") Collection<Long> taskIds);
}
