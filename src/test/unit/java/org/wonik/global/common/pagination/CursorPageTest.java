package org.wonik.global.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorPageTest {

	@Test
	@DisplayName("size + 1개를 받으면 hasNext=true이고 마지막으로 돌려준 항목 id가 다음 커서가 된다")
	void pageOf() {
		final CursorPage<Long> page = CursorPage.of(List.of(30L, 20L, 10L), 2, id -> id);

		assertThat(page.items()).containsExactly(30L, 20L);
		assertThat(page.hasNext()).isTrue();
		assertThat(CursorCodec.decode(page.nextCursor())).isEqualTo(20L);

		final CursorPage<Long> last = CursorPage.of(List.of(10L), 2, id -> id);
		assertThat(last.hasNext()).isFalse();
		assertThat(last.nextCursor()).isNull();
	}

	@Test
	@DisplayName("map은 항목만 바꾸고 커서 정보는 유지한다")
	void mapKeepsCursor() {
		final CursorPage<Long> page = CursorPage.of(List.of(30L, 20L, 10L), 2, id -> id);

		final CursorPage<String> mapped = page.map(id -> "#" + id);

		assertThat(mapped.items()).containsExactly("#30", "#20");
		assertThat(mapped.nextCursor()).isEqualTo(page.nextCursor());
		assertThat(mapped.hasNext()).isTrue();
	}
}
