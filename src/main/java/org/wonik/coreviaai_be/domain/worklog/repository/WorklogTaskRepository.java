package org.wonik.coreviaai_be.domain.worklog.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.worklog.entity.WorklogTask;

public interface WorklogTaskRepository extends JpaRepository<WorklogTask, Long> {
    Optional<WorklogTask> findByWorklogIdAndTaskId(Long worklogId, Long taskId);
    boolean existsByTaskId(Long taskId);
    @Query("select coalesce(sum(e.spentMinutes), 0) from WorklogTask e where e.taskId = :taskId")
    long totalMinutes(@Param("taskId") Long taskId);
    @Query("""
        select e from WorklogTask e join Worklog w on w.id = e.worklogId
        where w.userId = :userId and w.logDate = :date order by e.id
        """)
    Slice<WorklogTask> findOwned(@Param("userId") Long userId, @Param("date") LocalDate date, Pageable page);
    @Query("""
        select e from WorklogTask e join Worklog w on w.id = e.worklogId
        join WbsTask t on t.id = e.taskId
        where t.projectId = :projectId and w.logDate between :from and :to
        order by w.logDate desc, e.id desc
        """)
    Slice<WorklogTask> findProject(@Param("projectId") Long projectId,
        @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable page);
}
