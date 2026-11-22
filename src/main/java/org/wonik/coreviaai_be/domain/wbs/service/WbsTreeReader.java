package org.wonik.coreviaai_be.domain.wbs.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsTreeNode;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 WBS 트리를 조립한다. 작업·하위작업을 한 번에 읽고 담당자 이름을 한 번에 읽어 메모리에서 묶는다(쿼리 2회, N+1 없음).
 * 작업은 표시 순서(sort_order, 같으면 id)대로, 하위작업은 상위 작업 안에서 같은 기준으로 정렬한다.
 * 권한 판정은 호출자가 먼저 한다.
 */
@Component
@RequiredArgsConstructor
public class WbsTreeReader {

	private final WbsTaskRepository taskRepository;
	private final UserRepository userRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public List<WbsTreeNode> read(final Long projectId) {
		final List<WbsTask> tasks = taskRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId);
		final Map<Long, String> assigneeNames = assigneeNames(tasks);

		final Map<Long, List<WbsTreeNode>> subtasksByParent = new HashMap<>();
		for (final WbsTask task : tasks) {
			if (task.isSubtask()) {
				subtasksByParent.computeIfAbsent(task.getParentTaskId(), key -> new ArrayList<>())
					.add(new WbsTreeNode(task, assigneeNames.get(task.getAssigneeUserId()), List.of()));
			}
		}
		final List<WbsTreeNode> tree = new ArrayList<>();
		for (final WbsTask task : tasks) {
			if (!task.isSubtask()) {
				tree.add(new WbsTreeNode(task, assigneeNames.get(task.getAssigneeUserId()),
					List.copyOf(subtasksByParent.getOrDefault(task.getId(), List.of()))));
			}
		}
		return List.copyOf(tree);
	}

	private Map<Long, String> assigneeNames(final List<WbsTask> tasks) {
		final Set<Long> assigneeIds = tasks.stream()
			.map(WbsTask::getAssigneeUserId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		// 담당자 없는 항목은 null 키로 찾으므로 null 조회를 허용하는 HashMap을 쓴다(Map.of는 null 조회 시 NPE).
		final Map<Long, String> names = new HashMap<>();
		if (assigneeIds.isEmpty()) {
			return names;
		}
		for (final User user : userRepository.findAllById(assigneeIds)) {
			names.put(user.getId(), user.getName());
		}
		return names;
	}
}
