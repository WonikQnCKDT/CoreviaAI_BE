package org.wonik.coreviaai_be.domain.meeting.policy;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.meeting.entity.Meeting;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/** DOC-01 권한: 공개 범위 내 조회, 프로젝트 멤버 생성, 작성자 또는 PM 수정·삭제. */
@Component
@RequiredArgsConstructor
public class MeetingAccessPolicy {

	private final ProjectAccessPolicy projectAccessPolicy;
	private final ProjectFeatureAssignmentRepository featureRepository;
	private final ProjectMemberRepository memberRepository;

	public Project getReadableProject(final Long viewerId, final Long projectId) {
		final Project project = projectAccessPolicy.getVisibleProject(viewerId, projectId);
		requireFeature(projectId);
		return project;
	}

	public Project getWritableProject(final Long viewerId, final Long projectId) {
		final Project project = getReadableProject(viewerId, projectId);
		if (!memberRepository.existsByProjectIdAndUserId(projectId, viewerId)) {
			throw new CustomException(ErrorCode.MEETING_WRITE_FORBIDDEN);
		}
		return project;
	}

	public void requireEditor(final Long viewerId, final Meeting meeting) {
		getWritableProject(viewerId, meeting.getProjectId());
		if (!meeting.getAuthorUserId().equals(viewerId)
			&& !projectAccessPolicy.isPm(viewerId, meeting.getProjectId())) {
			throw new CustomException(ErrorCode.MEETING_WRITE_FORBIDDEN);
		}
	}

	private void requireFeature(final Long projectId) {
		if (!featureRepository.existsByProjectIdAndFeature(projectId, ProjectFeature.MEETING)) {
			throw new CustomException(ErrorCode.FEATURE_DISABLED);
		}
	}
}
