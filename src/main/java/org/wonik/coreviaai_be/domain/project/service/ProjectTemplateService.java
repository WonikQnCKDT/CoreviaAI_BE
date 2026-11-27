package org.wonik.coreviaai_be.domain.project.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.dto.request.CreateProjectRequest;
import org.wonik.coreviaai_be.domain.project.dto.response.CreationOptionsResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectTemplateResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.TemplateOptionResponse;
import org.wonik.coreviaai_be.domain.project.entity.ProjectTemplateDefinition;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.repository.ProjectTemplateRepository;
import org.wonik.coreviaai_be.domain.project.service.command.UpdateProjectTemplateCommand;
import org.wonik.coreviaai_be.domain.project.service.result.TemplateDefaults;
import org.wonik.coreviaai_be.domain.project.service.result.TemplateDefaults.RequiredDeliverable;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;

/**
 * 템플릿 기본값(FR-PRJ-09). 조회는 생성 화면이 쓰고, 수정은 시스템 관리자만 한다.
 * 프로젝트 행을 갱신하지 않으므로 수정은 이후 생성에만 적용된다.
 */
@Service
@RequiredArgsConstructor
public class ProjectTemplateService {

	static final String ACTIVE = "ACTIVE";

	private final ProjectTemplateRepository templateRepository;
	private final AuditLogger auditLogger;
	private final JsonMapper jsonMapper;

	@Transactional(readOnly = true)
	public CreationOptionsResponse creationOptions() {
		final List<TemplateOptionResponse> templates = templateRepository.findAllByStatusOrderByIdAsc(ACTIVE).stream()
			.map(template -> TemplateOptionResponse.from(template, readConfig(template)))
			.toList();
		return CreationOptionsResponse.of(templates);
	}

	@Transactional(readOnly = true)
	public List<String> defaultPhases(final ProjectTemplate template) {
		return readConfig(requireTemplate(template)).phases();
	}

	@Transactional(readOnly = true)
	public List<ProjectTemplateResponse> findAll() {
		return templateRepository.findAllByStatusOrderByIdAsc(ACTIVE).stream()
			.map(template -> ProjectTemplateResponse.from(template, readConfig(template)))
			.toList();
	}

	@Transactional
	public ProjectTemplateResponse update(final AuditActor actor, final UpdateProjectTemplateCommand command) {
		final ProjectTemplateDefinition template = templateRepository.findByTemplateKeyForUpdate(command.templateKey())
			.orElseThrow(() -> new CustomException(ErrorCode.TEMPLATE_NOT_FOUND));
		if (template.getVersion() != command.version()) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}
		final TemplateDefaults before = readConfig(template);
		final TemplateDefaults after = validate(command);
		final String beforeName = template.getName();
		final Methodology beforeMethodology = template.getMethodology();
		template.replaceDefaults(
			command.name().strip(),
			command.methodology(),
			command.departmentType(),
			writeConfig(after)
		);
		auditLogger.record(actor, AuditAction.PROJECT_TEMPLATE_CHANGED, AuditTargetType.PROJECT_TEMPLATE, template.getId(),
			AuditChanges.create()
				.value("templateKey", template.getTemplateKey())
				.change("name", beforeName, template.getName())
				.change("methodology", beforeMethodology, template.getMethodology())
				.change("phaseCount", before.phases().size(), after.phases().size()));
		return ProjectTemplateResponse.from(template, after);
	}

	private ProjectTemplateDefinition requireTemplate(final ProjectTemplate templateKey) {
		return templateRepository.findByTemplateKey(templateKey)
			.orElseThrow(() -> new CustomException(ErrorCode.TEMPLATE_NOT_FOUND));
	}

	private TemplateDefaults validate(final UpdateProjectTemplateCommand command) {
		if (!command.methodology().isEnabled()) {
			throw new CustomException(ErrorCode.METHODOLOGY_NOT_AVAILABLE);
		}
		if (command.departmentType() != null && !command.departmentType().isEnabled()) {
			throw new CustomException(ErrorCode.DEPARTMENT_TYPE_NOT_AVAILABLE);
		}
		if (!command.actors().contains(ProjectActor.PM)) {
			throw new CustomException(ErrorCode.PM_ACTOR_REQUIRED);
		}
		for (final ProjectActor actor : command.actors()) {
			final boolean allowed = command.departmentType() == null
				? actor.isCommon()
				: actor.selectableFor(command.departmentType());
			if (!allowed) {
				throw new CustomException(ErrorCode.ACTOR_NOT_AVAILABLE);
			}
		}
		final List<String> phases = normalizePhases(command.phases());
		final boolean hasPhases = !phases.isEmpty();
		for (final ProjectFeature feature : command.features()) {
			if (!feature.selectableFor(hasPhases)) {
				throw new CustomException(ErrorCode.FEATURE_NOT_AVAILABLE);
			}
		}
		final List<RequiredDeliverable> deliverables = normalizeDeliverables(command.requiredDeliverables(), phases);
		return new TemplateDefaults(
			phases,
			command.actors().stream().sorted().toList(),
			command.features().stream().sorted().toList(),
			deliverables
		);
	}

	private static List<String> normalizePhases(final List<String> phases) {
		if (phases.size() > CreateProjectRequest.MAX_PHASES) {
			throw new CustomException(ErrorCode.INVALID_TEMPLATE_CONFIG);
		}
		final List<String> normalized = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		for (final String phase : phases) {
			final String name = phase.strip();
			if (name.isEmpty() || name.length() > CreateProjectRequest.MAX_PHASE_NAME_LENGTH || !seen.add(name)) {
				throw new CustomException(ErrorCode.INVALID_TEMPLATE_CONFIG);
			}
			normalized.add(name);
		}
		return List.copyOf(normalized);
	}

	private static List<RequiredDeliverable> normalizeDeliverables(
		final List<RequiredDeliverable> deliverables,
		final List<String> phases
	) {
		final Set<String> phaseNames = new LinkedHashSet<>(phases);
		final Set<String> seen = new HashSet<>();
		final List<RequiredDeliverable> normalized = new ArrayList<>();
		for (final RequiredDeliverable deliverable : deliverables) {
			final String phaseName = deliverable.phaseName().strip();
			final String name = deliverable.name().strip();
			if (phaseName.isEmpty() || name.isEmpty() || name.length() > CreateProjectRequest.MAX_PHASE_NAME_LENGTH
				|| !phaseNames.contains(phaseName) || !seen.add(phaseName + "\n" + name)) {
				throw new CustomException(ErrorCode.INVALID_TEMPLATE_CONFIG);
			}
			normalized.add(new RequiredDeliverable(phaseName, name));
		}
		return List.copyOf(normalized);
	}

	private TemplateDefaults readConfig(final ProjectTemplateDefinition template) {
		try {
			final StoredConfig stored = jsonMapper.readValue(template.getDefaultConfig(), StoredConfig.class);
			return new TemplateDefaults(
				stored.phases() == null ? List.of() : List.copyOf(stored.phases()),
				enumsOf(stored.actors(), ProjectActor.class),
				enumsOf(stored.features(), ProjectFeature.class),
				stored.requiredDeliverables() == null ? List.of() : stored.requiredDeliverables().stream()
					.map(item -> new RequiredDeliverable(item.phaseName(), item.name()))
					.toList()
			);
		} catch (final JacksonException | IllegalArgumentException exception) {
			throw new CustomException(ErrorCode.INVALID_TEMPLATE_CONFIG, exception);
		}
	}

	private String writeConfig(final TemplateDefaults defaults) {
		return jsonMapper.writeValueAsString(new StoredConfig(
			defaults.phases(),
			defaults.actors().stream().map(Enum::name).toList(),
			defaults.features().stream().map(Enum::name).toList(),
			defaults.requiredDeliverables().stream()
				.map(item -> new StoredDeliverable(item.phaseName(), item.name()))
				.toList()
		));
	}

	private static <E extends Enum<E>> List<E> enumsOf(final List<String> names, final Class<E> type) {
		if (names == null) {
			return List.of();
		}
		return names.stream().map(name -> Enum.valueOf(type, name)).toList();
	}

	private record StoredConfig(
		List<String> phases,
		List<String> actors,
		List<String> features,
		List<StoredDeliverable> requiredDeliverables
	) {
	}

	private record StoredDeliverable(String phaseName, String name) {
	}
}
