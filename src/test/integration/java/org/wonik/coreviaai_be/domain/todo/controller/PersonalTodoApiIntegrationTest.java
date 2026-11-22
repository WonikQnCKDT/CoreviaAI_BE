package org.wonik.coreviaai_be.domain.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.coreviaai_be.domain.todo.repository.PersonalTodoRepository;
import org.wonik.coreviaai_be.domain.todo.service.PersonalTodoService;
import org.wonik.coreviaai_be.domain.todo.service.command.CreateTodoCommand;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalTodoApiIntegrationTest extends ProjectApiTestSupport {
	private static final String URL="/api/me/todos";
	private static final LocalDate TODAY=LocalDate.of(2026,11,9);
	@Autowired PersonalTodoRepository todos;
	@Autowired PersonalTodoService service;
	@Autowired WbsTaskRepository tasks;
	@Autowired JdbcTemplate jdbc;

	@Test void personalCreateUpdateCompleteAndReopen() throws Exception {
		final Cookie cookie=loginAs(developer);
		final JsonNode created=createTodo(cookie,"  Plan  ",null,UUID.randomUUID());
		final long id=created.get("id").asLong();
		assertThat(created.get("title").asString()).isEqualTo("Plan");
		assertThat(created.get("done").asBoolean()).isFalse();
		assertThat(created.get("doneAt").isNull()).isTrue();
		assertThat(items(cookie,false)).hasSize(1);
		final JsonNode completed=readResponse(patchJson(cookie,body("title","Plan","dueDate","2026-11-09","done",true),URL+"/{id}",id));
		final String doneAt=completed.get("doneAt").asString();
		clock.advance(Duration.ofSeconds(1));
		final JsonNode repeated=readResponse(patchJson(cookie,body("title","Plan","dueDate","2026-11-09","done",true),URL+"/{id}",id));
		assertThat(repeated.get("doneAt").asString()).isEqualTo(doneAt);
		assertThat(items(cookie,false)).isEmpty();
		assertThat(items(cookie,true)).hasSize(1);
		patchJson(cookie,body("title","Plan","dueDate",null,"done",false),URL+"/{id}",id).andExpect(status().isOk());
		assertThat(items(cookie,false)).hasSize(1);
		getAs(cookie,URL+"/{id}",id).andExpect(jsonPath("$.response.doneAt").isEmpty());
	}

	@Test void creationRetryIsScopedAndPreservesOriginalResponseAfterEdit() throws Exception {
		final Cookie cookie=loginAs(developer);
		final UUID requestId=UUID.randomUUID();
		final JsonNode first=createTodo(cookie,"Plan",null,requestId);
		assertThat(createTodo(cookie,"Plan",null,requestId)).isEqualTo(first);
		expectError(postJson(URL,cookie,body("requestId",requestId,"title","Other")),ErrorCode.TODO_REQUEST_CONFLICT);
		patchJson(cookie,body("title","Edited","done",true),URL+"/{id}",first.get("id").asLong()).andExpect(status().isOk());
		assertThat(createTodo(cookie,"Plan",null,requestId)).isEqualTo(first);
		createTodo(loginAs(outsider),"Plan",null,requestId);
		assertThat(todos.count()).isEqualTo(2);
	}

	@Test void creationRetryPreservesDatabaseTimestampPrecision() throws Exception {
		clock.setInstant(Instant.parse("2026-11-09T00:00:00.123456789Z"));
		final Cookie cookie=loginAs(developer);
		final UUID requestId=UUID.randomUUID();
		final JsonNode first=createTodo(cookie,"Precision",null,requestId);
		clock.advance(Duration.ofSeconds(1));
		assertThat(createTodo(cookie,"Precision",null,requestId)).isEqualTo(first);
	}

	@Test void completionRetryAndGetPreserveDatabaseTimestampPrecision() throws Exception {
		clock.setInstant(Instant.parse("2026-11-09T00:00:00.123456789Z"));
		final Cookie cookie=loginAs(developer);
		final long id=createTodo(cookie,"Completion precision",null,UUID.randomUUID()).get("id").asLong();
		final JsonNode first=readResponse(patchJson(cookie,body("title","Completion precision","done",true),URL+"/{id}",id));
		clock.advance(Duration.ofSeconds(1));
		final JsonNode repeated=readResponse(patchJson(cookie,body("title","Completion precision","done",true),URL+"/{id}",id));
		assertThat(repeated).isEqualTo(first);
		assertThat(readResponse(getAs(cookie,URL+"/{id}",id))).isEqualTo(first);
	}

	@Test void concurrentCreationRetriesCreateOneRow() throws Exception {
		final UUID requestId=UUID.randomUUID();
		final CountDownLatch ready=new CountDownLatch(2), start=new CountDownLatch(1);
		final var executor=Executors.newFixedThreadPool(2);
		try {
			final var action=(java.util.concurrent.Callable<Long>)() -> {
				ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) { throw new AssertionError("start timeout"); }
				return service.create(developer.getId(),new CreateTodoCommand(requestId,"Concurrent",null)).id();
			};
			final var one=executor.submit(action); final var two=executor.submit(action);
			assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
			assertThat(one.get(20,TimeUnit.SECONDS)).isEqualTo(two.get(20,TimeUnit.SECONDS));
			assertThat(todos.count()).isEqualTo(1);
		} finally { start.countDown(); executor.shutdownNow(); }
	}

	@Test void inputValidationIncludesTitleUuidDateAndCompletion() throws Exception {
		final Cookie cookie=loginAs(developer);
		for(final String title:new String[]{"","  ","x".repeat(201),"bad" + (char)0 + "title", "line\nbreak"}) {
			postJson(URL,cookie,body("requestId",UUID.randomUUID(),"title",title)).andExpect(status().isBadRequest());
		}
		createTodo(cookie,"x".repeat(200),"0001-01-01",UUID.randomUUID());
		createTodo(cookie,"End","9999-12-31",UUID.randomUUID());
		expectError(postJson(URL,cookie,body("requestId",UUID.randomUUID(),"title","Date","dueDate","0000-01-01")),ErrorCode.INVALID_TODO_DATE);
		postJson(URL,cookie,body("title","Missing key")).andExpect(status().isBadRequest());
		postJson(URL,cookie,body("requestId","bad-uuid","title","Invalid key")).andExpect(status().isBadRequest());
		postJson(URL,cookie,body("requestId",UUID.randomUUID(),"title","Date","dueDate","2026-02-30")).andExpect(status().isBadRequest());
		final long id=createTodo(cookie,"Patch",null,UUID.randomUUID()).get("id").asLong();
		patchJson(cookie,body("title","Patch"),URL+"/{id}",id).andExpect(status().isBadRequest());
		getAs(cookie,URL+"/0").andExpect(status().isBadRequest());
	}

	@Test void personalDataIsPrivateForOtherUsersAndSystemAdmins() throws Exception {
		final Cookie cookie=loginAs(developer);
		final long id=createTodo(cookie,"Private",null,UUID.randomUUID()).get("id").asLong();
		final var admin=saveUser("admin01",USER_PASSWORD,SystemRole.SYSTEM_ADMIN,null);
		for(final Cookie other:new Cookie[]{loginAs(outsider),loginAs(admin)}) {
			expectError(getAs(other,URL+"/{id}",id),ErrorCode.TODO_NOT_FOUND);
			expectError(patchJson(other,body("title","Hacked","done",true),URL+"/{id}",id),ErrorCode.TODO_NOT_FOUND);
			assertThat(items(other,false)).isEmpty();
		}
		createTodo(loginAs(admin),"Admin personal",null,UUID.randomUUID());
		assertThat(items(loginAs(admin),false)).hasSize(1);
		getAs(null,URL+"?dueSoonDays=3").andExpect(status().isUnauthorized());
	}

	@Test void feedMergesLiveTasksAndPersonalRowsInStableKeysetOrder() throws Exception {
		final long projectId=createProject(validCreateBody("TODOSORT"));
		final Cookie cookie=loginAs(developer);
		createTodo(cookie,"No due",null,UUID.randomUUID());
		createTodo(cookie,"Later",TODAY.plusDays(4).toString(),UUID.randomUUID());
		createTodo(cookie,"Overdue",TODAY.minusDays(1).toString(),UUID.randomUUID());
		createTodo(cookie,"Soon",TODAY.plusDays(3).toString(),UUID.randomUUID());
		createTodo(cookie,"Today personal",TODAY.toString(),UUID.randomUUID());
		saveTask(projectId,developer.getId(),"Today task",TODAY);
		String cursor=null;
		final var titles=new java.util.ArrayList<String>();
		do {
			final JsonNode page=readResponse(getAs(cookie,URL+"?dueSoonDays=3&size=1"+(cursor==null?"":"&cursor="+cursor)));
			page.get("items").valueStream().forEach(item -> titles.add(item.get("title").asString()));
			cursor=page.get("nextCursor").isNull()?null:page.get("nextCursor").asString();
		} while(cursor!=null);
		assertThat(titles).containsExactly("Today personal","Today task","Soon","Overdue","Later","No due");
		final JsonNode late=items(cookie,false).stream().filter(i -> i.get("title").asString().equals("Overdue")).findFirst().orElseThrow();
		assertThat(late.get("overdue").asBoolean()).isTrue();
	}

	@Test void assignmentCompletionAndUnassignmentAreReflectedWithoutReplicationDelay() throws Exception {
		final long projectId=createProject(validCreateBody("TODOLIVE"));
		final Cookie cookie=loginAs(developer);
		final WbsTask task=saveTask(projectId,developer.getId(),"Assigned",TODAY);
		assertThat(items(cookie,false)).hasSize(1);
		jdbc.update("update wbs_tasks set status='DONE' where id=?",task.getId());
		assertThat(items(cookie,false)).isEmpty();
		jdbc.update("update wbs_tasks set status='TODO', assignee_user_id=null where id=?",task.getId());
		assertThat(items(cookie,false)).isEmpty();
		jdbc.update("update wbs_tasks set assignee_user_id=? where id=?",developer.getId(),task.getId());
		assertThat(items(cookie,false)).hasSize(1);
		jdbc.update("delete from project_members where project_id=? and user_id=?",projectId,developer.getId());
		assertThat(items(cookie,false)).isEmpty();
	}

	@Test void promotionToSystemAdminDoesNotBypassProjectDataBoundary() throws Exception {
		final long projectId=createProject(validCreateBody("TODOROLE"));
		final Cookie cookie=loginAs(developer);
		saveTask(projectId,developer.getId(),"Assigned",TODAY);
		createTodo(cookie,"Personal",null,UUID.randomUUID());
		assertThat(items(cookie,false)).hasSize(2);
		jdbc.update("update users set system_role='SYSTEM_ADMIN' where id=?",developer.getId());
		assertThat(items(cookie,false)).extracting(i -> i.get("source").asString()).containsExactly("PERSONAL");
	}

	@Test void executiveTaskFeedUsesExistingProjectDepartmentScope() throws Exception {
		final var request=validCreateBody("TODOEXEC");request.put("visibility","MEMBERS_ONLY");
		final long projectId=createProject(request);
		jdbc.update("insert into project_members(project_id,user_id,actor,created_at,updated_at) values (?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",projectId,itExecutive.getId(),"DEVELOPER");
		saveTask(projectId,itExecutive.getId(),"Executive assigned",TODAY);
		final Cookie cookie=loginAs(itExecutive);
		assertThat(items(cookie,false)).hasSize(1);
		jdbc.update("delete from project_departments where project_id=?",projectId);
		assertThat(items(cookie,false)).isEmpty();
		jdbc.update("update projects set visibility='COMPANY' where id=?",projectId);
		assertThat(items(cookie,false)).hasSize(1);
	}

	@Test void paginationValidationAndCursorScope() throws Exception {
		final Cookie cookie=loginAs(developer);
		createTodo(cookie,"One",null,UUID.randomUUID());createTodo(cookie,"Two",null,UUID.randomUUID());
		final String cursor=readResponse(getAs(cookie,URL+"?dueSoonDays=3&size=1")).get("nextCursor").asString();
		expectError(getAs(loginAs(outsider),URL+"?dueSoonDays=3&cursor="+cursor),ErrorCode.INVALID_CURSOR);
		expectError(getAs(cookie,URL+"?dueSoonDays=4&cursor="+cursor),ErrorCode.INVALID_CURSOR);
		expectError(getAs(cookie,URL+"?dueSoonDays=3&done=true&cursor="+cursor),ErrorCode.INVALID_CURSOR);
		expectError(getAs(cookie,URL+"?dueSoonDays=3&cursor=bad"),ErrorCode.INVALID_CURSOR);
		for(final String query:new String[]{"dueSoonDays=3&size=0","dueSoonDays=3&size=101","dueSoonDays=0","dueSoonDays=366"}) {
			getAs(cookie,URL+"?"+query).andExpect(status().isBadRequest());
		}
	}

	@Test void kstMidnightChangesFreshOrderingButContinuationKeepsItsOrderingDay() throws Exception {
		clock.setInstant(Instant.parse("2026-11-09T14:59:59Z"));
		final Cookie cookie=loginAs(developer);
		createTodo(cookie,"Due tomorrow","2026-11-10",UUID.randomUUID());
		createTodo(cookie,"Due today","2026-11-09",UUID.randomUUID());
		final JsonNode first=readResponse(getAs(cookie,URL+"?dueSoonDays=3&size=1"));
		assertThat(first.get("items").get(0).get("title").asString()).isEqualTo("Due today");
		clock.advance(Duration.ofSeconds(1));
		final JsonNode next=readResponse(getAs(cookie,URL+"?dueSoonDays=3&size=1&cursor="+first.get("nextCursor").asString()));
		assertThat(next.get("items").get(0).get("title").asString()).isEqualTo("Due tomorrow");
		assertThat(next.get("items").get(0).get("urgency").asInt()).isEqualTo(1);
		assertThat(items(cookie,false).get(0).get("title").asString()).isEqualTo("Due tomorrow");
	}

	@Test void identicalNumericIdsAcrossSourcesStillPaginate() throws Exception {
		final long projectId=createProject(validCreateBody("TODOID"));
		final Cookie cookie=loginAs(developer);
		final WbsTask task=saveTask(projectId,developer.getId(),"Task",TODAY);
		final long personalId=createTodo(cookie,"Personal",TODAY.toString(),UUID.randomUUID()).get("id").asLong();
		jdbc.update("update personal_todos set id=? where id=?",task.getId(),personalId);
		final JsonNode first=readResponse(getAs(cookie,URL+"?dueSoonDays=3&size=1"));
		assertThat(first.get("items").get(0).get("source").asString()).isEqualTo("PERSONAL");
		final JsonNode second=readResponse(getAs(cookie,URL+"?dueSoonDays=3&size=1&cursor="+first.get("nextCursor").asString()));
		assertThat(second.get("items").get(0).get("source").asString()).isEqualTo("TASK");
		assertThat(second.get("items").get(0).get("id").asLong()).isEqualTo(task.getId());
		assertThat(second.get("hasNext").asBoolean()).isFalse();
	}

	@Test void existingCursorCannotRetainRevokedProjectAccess() throws Exception {
		final long projectId=createProject(validCreateBody("TODOREVOKE"));
		final Cookie cookie=loginAs(developer);
		saveTask(projectId,developer.getId(),"First",TODAY);saveTask(projectId,developer.getId(),"Second",TODAY);
		final String cursor=readResponse(getAs(cookie,URL+"?dueSoonDays=3&size=1")).get("nextCursor").asString();
		jdbc.update("delete from project_members where project_id=? and user_id=?",projectId,developer.getId());
		assertThat(readResponse(getAs(cookie,URL+"?dueSoonDays=3&cursor="+cursor)).get("items")).isEmpty();
		jdbc.update("insert into project_members(project_id,user_id,actor,created_at,updated_at) values (?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",projectId,developer.getId(),"DEVELOPER");
		jdbc.update("update users set system_role='SYSTEM_ADMIN' where id=?",developer.getId());
		assertThat(readResponse(getAs(cookie,URL+"?dueSoonDays=3&cursor="+cursor)).get("items")).isEmpty();
	}

	private JsonNode createTodo(final Cookie cookie,final String title,final String dueDate,final UUID requestId) throws Exception {
		final var result=postJson(URL,cookie,body("requestId",requestId,"title",title,"dueDate",dueDate))
			.andExpect(status().isCreated()).andReturn();
		return jsonMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("response");
	}
	private List<JsonNode> items(final Cookie cookie,final boolean done) throws Exception {
		return readResponse(getAs(cookie,URL+"?dueSoonDays=3&size=100&done="+done)).get("items").valueStream().toList();
	}
	private WbsTask saveTask(final long projectId,final Long assignee,final String title,final LocalDate dueDate) {
		return tasks.saveAndFlush(WbsTask.task(projectId,1,new TaskDetails(title,null,assignee,dueDate,dueDate,BigDecimal.ONE),WbsItemSource.MANUAL,creator.getId()));
	}
}
