package org.wonik.coreviaai_be.domain.todo.pagination;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.todo.service.result.TodoItem;
import org.wonik.global.common.exception.CustomException;
class TodoCursorTest {
	private static final LocalDate TODAY=LocalDate.of(2026,11,21);
	private final TodoItem item=new TodoItem(2L,"TASK","Plan",TODAY,false,null,1L,"TODO",0,false);
	@Test void roundTripWithScope() {
		final String value=TodoCursor.encode(1L,false,3,TODAY,item);
		assertThat(TodoCursor.decode(value,1L,false,3)).isEqualTo(new TodoCursor(TODAY,0,TODAY,1,2L));
		assertThatThrownBy(() -> TodoCursor.decode(value,2L,false,3)).isInstanceOf(CustomException.class);
		assertThatThrownBy(() -> TodoCursor.decode(value,1L,true,3)).isInstanceOf(CustomException.class);
		assertThatThrownBy(() -> TodoCursor.decode(value,1L,false,7)).isInstanceOf(CustomException.class);
	}
	@Test void malformedCursorAndLength() {
		for(final String cursor:new String[]{"!", "x".repeat(257),Base64.getUrlEncoder().encodeToString("todo1|1|false|3|bad|0|bad|1|2".getBytes())}) {
			assertThatThrownBy(() -> TodoCursor.decode(cursor,1L,false,3)).isInstanceOf(CustomException.class);
		}
		assertThat(TodoCursor.decode(null,1L,false,3)).isNull();
		assertThat(TodoCursor.decode(" ",1L,false,3)).isNull();
	}
	@Test void missingDateUsesLastSortKey() {
		final TodoItem noDate=new TodoItem(1L,"PERSONAL","Plan",null,false,null,null,null,4,false);
		final TodoCursor decoded=TodoCursor.decode(TodoCursor.encode(1L,false,3,TODAY,noDate),1L,false,3);
		assertThat(decoded.dueSort()).isEqualTo(TodoCursor.NO_DUE_DATE);
	}
}
