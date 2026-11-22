package org.wonik.coreviaai_be.domain.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.coreviaai_be.domain.todo.repository.PersonalTodoRepository;
import org.wonik.coreviaai_be.domain.todo.service.PersonalTodoService;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalTodoSoftDeleteApiIntegrationTest extends ProjectApiTestSupport {
	private static final String URL="/api/me/todos";
	@Autowired PersonalTodoRepository todos;
	@Autowired PersonalTodoService service;
	@Autowired WbsTaskRepository tasks;
	@MockitoSpyBean AuditLogger auditLogger;

	@Test void ownDeleteIsPrivateIdempotentAndAuditsOnlyMinimalData() throws Exception {
		clock.setInstant(Instant.parse("2026-11-09T00:00:00.123456789Z"));
		final Cookie cookie=loginAs(developer);
		final UUID key=UUID.randomUUID();
		final long id=createTodo(cookie,"Private sensitive title",null,key).get("id").asLong();
		final var admin=saveUser("admin01",USER_PASSWORD,SystemRole.SYSTEM_ADMIN,null);
		for(final Cookie other:new Cookie[]{loginAs(outsider),loginAs(admin)}) {
			expectError(deleteAs(other,URL+"/{id}",id),ErrorCode.TODO_NOT_FOUND);
		}
		deleteAs(null,URL+"/{id}",id).andExpect(status().isUnauthorized());
		deleteAs(cookie,URL+"/0").andExpect(status().isBadRequest());
		deleteAs(cookie,URL+"/{id}",id).andExpect(status().isNoContent()).andExpect(content().string(""));
		final var tombstone=todos.findById(id).orElseThrow();
		assertThat(tombstone.getDeletedAt()).isNotNull();
		assertThat(tombstone.getRequestId()).isEqualTo(key.toString());
		deleteAs(cookie,URL+"/{id}",id).andExpect(status().isNoContent());
		assertThat(todos.findById(id).orElseThrow().getDeletedAt()).isEqualTo(tombstone.getDeletedAt());
		assertThat(todos.count()).isEqualTo(1);
		expectError(getAs(cookie,URL+"/{id}",id),ErrorCode.TODO_NOT_FOUND);
		expectError(patchJson(cookie,body("title","Revive","done",false),URL+"/{id}",id),ErrorCode.TODO_NOT_FOUND);
		expectError(deleteAs(cookie,URL+"/999999999"),ErrorCode.TODO_NOT_FOUND);
		final var logs=auditLogs(AuditAction.PERSONAL_TODO_DELETED);
		assertThat(logs).hasSize(1);
		final var log=logs.get(0);
		assertThat(log.getTargetType()).isEqualTo(AuditTargetType.PERSONAL_TODO);
		assertThat(log.getTargetId()).isEqualTo(id);
		assertThat(log.getActorUserId()).isEqualTo(developer.getId());
		final JsonNode details=jsonMapper.readTree(log.getDetails());
		assertThat(details.size()).isEqualTo(1);
		assertThat(details.get("ownerUserId").asLong()).isEqualTo(developer.getId());
		assertThat(log.getDetails()).doesNotContain("sensitive",key.toString(),tombstone.getRequestHash());
	}

	@Test void completedAndOpenListsExcludeTombstonesAndAdminCanDeleteOwnItem() throws Exception {
		final var admin=saveUser("admin01",USER_PASSWORD,SystemRole.SYSTEM_ADMIN,null);
		final Cookie cookie=loginAs(admin);
		final long openId=createTodo(cookie,"Open",null,UUID.randomUUID()).get("id").asLong();
		final long completedId=createTodo(cookie,"Completed",null,UUID.randomUUID()).get("id").asLong();
		patchJson(cookie,body("title","Completed","done",true),URL+"/{id}",completedId).andExpect(status().isOk());
		assertThat(items(cookie,false)).hasSize(1);assertThat(items(cookie,true)).hasSize(1);
		deleteAs(cookie,URL+"/{id}",openId).andExpect(status().isNoContent());
		deleteAs(cookie,URL+"/{id}",completedId).andExpect(status().isNoContent());
		assertThat(items(cookie,false)).isEmpty();assertThat(items(cookie,true)).isEmpty();
		assertThat(todos.findById(completedId).orElseThrow().isDone()).isTrue();
		assertThat(todos.findById(completedId).orElseThrow().getDoneAt()).isNotNull();
		assertThat(todos.count()).isEqualTo(2);
	}

	@Test void deletedCreationKeyCannotResurrectOrDuplicateResource() throws Exception {
		final Cookie cookie=loginAs(developer);
		final UUID key=UUID.randomUUID();
		final long id=createTodo(cookie,"Original",null,key).get("id").asLong();
		deleteAs(cookie,URL+"/{id}",id).andExpect(status().isNoContent());
		expectError(postJson(URL,cookie,body("title","Original","requestId",key)),ErrorCode.TODO_DELETED);
		expectError(postJson(URL,cookie,body("title","Different","requestId",key)),ErrorCode.TODO_REQUEST_CONFLICT);
		assertThat(todos.count()).isEqualTo(1);
		assertThat(todos.findById(id).orElseThrow().getDeletedAt()).isNotNull();
		createTodo(cookie,"New item",null,UUID.randomUUID());
		createTodo(loginAs(outsider),"Other owner",null,key);
		assertThat(todos.count()).isEqualTo(3);
	}

	@Test void concurrentDeleteRequestsAuditOnce() throws Exception {
		final Cookie cookie=loginAs(developer);
		final long id=createTodo(cookie,"Concurrent delete",null,UUID.randomUUID()).get("id").asLong();
		final CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
		final var executor=Executors.newFixedThreadPool(2);
		try {
			final var action=(java.util.concurrent.Callable<Boolean>)() -> {
				ready.countDown();if(!start.await(10,TimeUnit.SECONDS)) { throw new AssertionError("start timeout"); }
				service.delete(new AuditActor(developer.getId(),null),id);return true;
			};
			final var one=executor.submit(action);final var two=executor.submit(action);
			assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();start.countDown();
			assertThat(one.get(20,TimeUnit.SECONDS)).isTrue();assertThat(two.get(20,TimeUnit.SECONDS)).isTrue();
			assertThat(auditLogs(AuditAction.PERSONAL_TODO_DELETED)).hasSize(1);
			assertThat(todos.findById(id).orElseThrow().getDeletedAt()).isNotNull();
		} finally {start.countDown();executor.shutdownNow();}
	}

	@Test void auditFailureRollsBackDeletionAndRetryStillSucceeds() throws Exception {
		final Cookie cookie=loginAs(developer);
		final long id=createTodo(cookie,"Audit atomicity",null,UUID.randomUUID()).get("id").asLong();
		final AuditLogger target = AopTestUtils.getUltimateTargetObject(auditLogger);
		doThrow(new IllegalStateException("injected audit failure")).when(target).record(any(),
			eq(AuditAction.PERSONAL_TODO_DELETED),eq(AuditTargetType.PERSONAL_TODO),eq(id),any());
		deleteAs(cookie,URL+"/{id}",id).andExpect(status().isInternalServerError());
		assertThat(todos.findById(id).orElseThrow().getDeletedAt()).isNull();
		assertThat(auditLogs(AuditAction.PERSONAL_TODO_DELETED)).isEmpty();
		getAs(cookie,URL+"/{id}",id).andExpect(status().isOk());
		org.mockito.Mockito.reset(target);
		deleteAs(cookie,URL+"/{id}",id).andExpect(status().isNoContent());
		assertThat(auditLogs(AuditAction.PERSONAL_TODO_DELETED)).hasSize(1);
	}

	@Test void existingCursorExcludesDeletedPersonalRowAndKeepsWbsTask() throws Exception {
		final long projectId=createProject(validCreateBody("TODODELETE"));
		final Cookie cookie=loginAs(developer);
		createTodo(cookie,"First","2026-11-09",UUID.randomUUID());
		final long second=createTodo(cookie,"Second","2026-11-09",UUID.randomUUID()).get("id").asLong();
		final LocalDate day=LocalDate.of(2026,11,9);
		final WbsTask task=tasks.saveAndFlush(WbsTask.task(projectId,1,
			new TaskDetails("WBS remains",null,developer.getId(),day,day,BigDecimal.ONE),WbsItemSource.MANUAL,creator.getId()));
		final JsonNode first=readResponse(getAs(cookie,URL+"?size=1"));
		final String cursor=first.get("nextCursor").asString();
		deleteAs(cookie,URL+"/{id}",second).andExpect(status().isNoContent());
		final JsonNode next=readResponse(getAs(cookie,URL+"?cursor="+cursor));
		assertThat(next.get("items").valueStream().map(i -> i.get("source").asString()).toList()).containsExactly("TASK");
		assertThat(tasks.findById(task.getId())).isPresent();
		assertThat(items(cookie,false)).hasSize(2);
	}

	@Test void defaultDueSoonWindowIsThreeDaysAndExplicitOverridesRemainCompatible() throws Exception {
		final Cookie cookie=loginAs(developer);
		createTodo(cookie,"Today","2026-11-09",UUID.randomUUID());
		createTodo(cookie,"Day one","2026-11-10",UUID.randomUUID());
		createTodo(cookie,"Day three","2026-11-12",UUID.randomUUID());
		createTodo(cookie,"Day four","2026-11-13",UUID.randomUUID());
		final JsonNode defaults=readResponse(getAs(cookie,URL));
		assertThat(defaults).isEqualTo(readResponse(getAs(cookie,URL+"?dueSoonDays=3")));
		assertThat(defaults.get("items").valueStream().map(i -> i.get("urgency").asInt()).toList()).containsExactly(0,1,1,3);
		final JsonNode override=readResponse(getAs(cookie,URL+"?dueSoonDays=1"));
		assertThat(override.get("items").valueStream().map(i -> i.get("urgency").asInt()).toList()).containsExactly(0,1,3,3);
		final String cursor=readResponse(getAs(cookie,URL+"?size=1")).get("nextCursor").asString();
		assertThat(readResponse(getAs(cookie,URL+"?dueSoonDays=3&cursor="+cursor)))
			.isEqualTo(readResponse(getAs(cookie,URL+"?cursor="+cursor)));
		expectError(getAs(cookie,URL+"?dueSoonDays=4&cursor="+cursor),ErrorCode.INVALID_CURSOR);
		for(final int invalid:new int[]{0,366}) { getAs(cookie,URL+"?dueSoonDays="+invalid).andExpect(status().isBadRequest()); }
	}

	private JsonNode createTodo(final Cookie cookie,final String title,final String dueDate,final UUID key) throws Exception {
		final var result=postJson(URL,cookie,body("title",title,"dueDate",dueDate,"requestId",key))
			.andExpect(status().isCreated()).andReturn();
		return jsonMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("response");
	}
	private List<JsonNode> items(final Cookie cookie,final boolean done) throws Exception {
		return readResponse(getAs(cookie,URL+"?done="+done)).get("items").valueStream().toList();
	}
}
