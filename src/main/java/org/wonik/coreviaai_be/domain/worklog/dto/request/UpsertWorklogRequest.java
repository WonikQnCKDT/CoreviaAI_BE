package org.wonik.coreviaai_be.domain.worklog.dto.request;

import org.wonik.coreviaai_be.domain.worklog.entity.Worklog;
import jakarta.validation.constraints.Size;

/** 업무일지 저장 요청. 날짜는 경로에서 받는다. content를 비우면 내용이 없는 업무일지가 된다. */
public record UpsertWorklogRequest(@Size(max = Worklog.CONTENT_MAX_LENGTH) String content) {

	@Override
	public String toString() {
		return "UpsertWorklogRequest[redacted]";
	}
}
