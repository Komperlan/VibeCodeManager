package com.aiq.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiq.application.port.out.AiToolRepository;
import com.aiq.application.port.out.ProjectRepository;
import com.aiq.application.port.out.PromptExecutionRepository;
import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.port.out.PromptRepository;
import com.aiq.domain.aitool.AiTool;
import com.aiq.domain.aitool.AiToolType;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.domain.execution.ExecutionStatus;
import com.aiq.domain.execution.PromptExecution;
import com.aiq.domain.project.Project;
import com.aiq.domain.queue.AutoRunMode;
import com.aiq.domain.queue.Prompt;
import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.PromptStatus;
import com.aiq.domain.queue.QueueExecutionPolicy;
import com.aiq.domain.queue.QueueStatus;
import com.aiq.domain.safety.WorkingHours;
import com.aiq.infrastructure.persistence.entity.ProjectJpaEntity;
import com.aiq.infrastructure.persistence.repositories.ProjectJpaRepository;
import com.aiq.infrastructure.testsupport.DockerAvailableCondition;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@ExtendWith(DockerAvailableCondition.class)
@SpringBootTest(classes = PersistenceAdapterIntegrationTest.PersistenceTestConfiguration.class)
class PersistenceAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AiToolRepository aiToolRepository;

    @Autowired
    private PromptQueueRepository promptQueueRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private PromptExecutionRepository promptExecutionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
            TRUNCATE TABLE
                prompt_executions,
                prompts,
                prompt_queues,
                ai_tools,
                projects
            RESTART IDENTITY CASCADE
            """);
    }

    @Test
    void projectRepositoryShouldSaveAndLoadProjects() {
        Project savedProject = projectRepository.save(Project.create("Backend", "/tmp/backend"));

        assertThat(projectRepository.existsByRootDirectory("/tmp/backend")).isTrue();
        assertThat(projectRepository.existsByRootDirectory("/tmp/missing")).isFalse();
        assertThat(projectRepository.findById(savedProject.getId()))
            .hasValueSatisfying(project -> {
                assertThat(project.getName()).isEqualTo("Backend");
                assertThat(project.getRootDirectory()).isEqualTo("/tmp/backend");
                assertThat(project.isActive()).isTrue();
            });
        assertThat(projectRepository.findAll())
            .extracting(Project::getId)
            .containsExactly(savedProject.getId());
    }

    @Test
    void aiToolRepositoryShouldSaveAndListTools() {
        AiTool savedTool = aiToolRepository.save(AiTool.create("Fake executor", AiToolType.FAKE, "/usr/bin/fake"));

        assertThat(aiToolRepository.findById(savedTool.getId()))
            .hasValueSatisfying(tool -> {
                assertThat(tool.getName()).isEqualTo("Fake executor");
                assertThat(tool.getType()).isEqualTo(AiToolType.FAKE);
                assertThat(tool.isEnabled()).isTrue();
            });
        assertThat(aiToolRepository.findAll())
            .extracting(AiTool::getId)
            .containsExactly(savedTool.getId());
    }

    @Test
    void promptQueueRepositoryShouldPersistPolicyAndFilterByProjectAndStatus() {
        Project project = savedProject("queue-project", "/tmp/queue-project");
        QueueExecutionPolicy policy = new QueueExecutionPolicy(
            AutoRunMode.AUTO_RUN,
            5,
            Duration.ofSeconds(30),
            false,
            true,
            new WorkingHours(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                ZoneId.of("Europe/Moscow")
            )
        );
        PromptQueue queue = promptQueueRepository.save(PromptQueue.create(project.getId(), "Main queue", policy));
        PromptQueue disabledQueue = PromptQueue.create(project.getId(), "Disabled queue", QueueExecutionPolicy.defaultPolicy());
        disabledQueue.disable();
        disabledQueue = promptQueueRepository.save(disabledQueue);

        assertThat(promptQueueRepository.findByProjectId(project.getId()))
            .extracting(PromptQueue::getId)
            .containsExactlyInAnyOrder(queue.getId(), disabledQueue.getId());
        assertThat(promptQueueRepository.findByStatuses(Set.of(QueueStatus.CREATED)))
            .extracting(PromptQueue::getId)
            .containsExactly(queue.getId());
        assertThat(promptQueueRepository.findById(queue.getId()))
            .hasValueSatisfying(foundQueue -> {
                QueueExecutionPolicy restoredPolicy = foundQueue.getExecutionPolicy();
                assertThat(restoredPolicy.autoRunMode()).isEqualTo(AutoRunMode.AUTO_RUN);
                assertThat(restoredPolicy.maxPromptsPerRun()).isEqualTo(5);
                assertThat(restoredPolicy.cooldown()).isEqualTo(Duration.ofSeconds(30));
                assertThat(restoredPolicy.stopOnError()).isFalse();
                assertThat(restoredPolicy.workingHours().zoneId()).isEqualTo(ZoneId.of("Europe/Moscow"));
            });
    }

    @Test
    void promptQueueRepositoryShouldHoldPessimisticLockUntilTransactionCompletes() throws Exception {
        PromptQueue queue = savedQueueFixture().queue();
        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstLock = new CountDownLatch(1);
        AtomicBoolean secondLockAcquired = new AtomicBoolean(false);
        var executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);

        try {
            var first = executor.submit(() -> transactions.executeWithoutResult(status -> {
                assertThat(promptQueueRepository.findByIdForUpdate(queue.getId())).isPresent();
                firstLockAcquired.countDown();
                await(releaseFirstLock);
            }));
            assertThat(firstLockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            var second = executor.submit(() -> transactions.executeWithoutResult(status -> {
                assertThat(promptQueueRepository.findByIdForUpdate(queue.getId())).isPresent();
                secondLockAcquired.set(true);
            }));

            Thread.sleep(200);
            assertThat(secondLockAcquired).isFalse();

            releaseFirstLock.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertThat(secondLockAcquired).isTrue();
        } finally {
            releaseFirstLock.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void promptRepositoryShouldPersistPromptsAndCalculateNextPosition() {
        QueueFixture fixture = savedQueueFixture();

        assertThat(promptRepository.nextPosition(fixture.queue().getId())).isZero();

        Prompt queuedPrompt = promptRepository.save(Prompt.createQueued(
            fixture.queue().getId(),
            fixture.aiTool().getId(),
            "First prompt",
            "Implement first task",
            10,
            promptRepository.nextPosition(fixture.queue().getId()),
            3,
            "/tmp/workdir"
        ));
        Prompt draftPrompt = promptRepository.save(Prompt.createDraft(
            fixture.queue().getId(),
            fixture.aiTool().getId(),
            "Draft prompt",
            "Think about second task",
            1,
            promptRepository.nextPosition(fixture.queue().getId()),
            2,
            null
        ));

        assertThat(queuedPrompt.getPosition()).isZero();
        assertThat(draftPrompt.getPosition()).isEqualTo(1);
        assertThat(promptRepository.nextPosition(fixture.queue().getId())).isEqualTo(2);
        assertThat(promptRepository.findByQueueId(fixture.queue().getId()))
            .extracting(Prompt::getId)
            .containsExactlyInAnyOrder(queuedPrompt.getId(), draftPrompt.getId());
        assertThat(promptRepository.findByQueueIdAndStatus(fixture.queue().getId(), PromptStatus.QUEUED))
            .extracting(Prompt::getId)
            .containsExactly(queuedPrompt.getId());
        assertThat(promptRepository.findById(queuedPrompt.getId()))
            .hasValueSatisfying(prompt -> assertThat(prompt.workingDirectoryOverride()).contains("/tmp/workdir"));
    }

    @Test
    void promptExecutionRepositoryShouldPersistResultsAndFindLatestExecution() {
        PromptFixture fixture = savedPromptFixture();
        Instant startedAt = Instant.parse("2026-06-16T10:00:00Z");
        PromptExecution firstExecution = execution(
            fixture.prompt().getId(),
            fixture.aiTool().getId(),
            "fake first",
            startedAt,
            startedAt.plusMillis(100)
        );
        PromptExecution latestExecution = execution(
            fixture.prompt().getId(),
            fixture.aiTool().getId(),
            "fake latest",
            startedAt.plusSeconds(1),
            startedAt.plusSeconds(1).plusMillis(250)
        );

        promptExecutionRepository.save(firstExecution);
        promptExecutionRepository.save(latestExecution);

        assertThat(promptExecutionRepository.findByPromptId(fixture.prompt().getId()))
            .extracting(PromptExecution::getId)
            .containsExactlyInAnyOrder(firstExecution.getId(), latestExecution.getId());
        assertThat(promptExecutionRepository.findLatestByPromptId(fixture.prompt().getId()))
            .hasValueSatisfying(execution -> assertThat(execution.getId()).isEqualTo(latestExecution.getId()));
        assertThat(promptExecutionRepository.findById(firstExecution.getId()))
            .hasValueSatisfying(execution -> {
                assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
                assertThat(execution.result()).hasValueSatisfying(result -> assertThat(result.stdout()).isEqualTo("done"));
                assertThat(execution.duration()).contains(Duration.ofMillis(100));
            });
    }

    private PromptExecution execution(UUID promptId, UUID aiToolId, String command, Instant startedAt, Instant finishedAt) {
        return PromptExecution.restore(
            UUID.randomUUID(),
            promptId,
            aiToolId,
            ExecutionStatus.COMPLETED,
            command,
            new ExecutionResult(0, "done", "", "done", null),
            startedAt,
            finishedAt,
            Duration.between(startedAt, finishedAt)
        );
    }

    private PromptFixture savedPromptFixture() {
        QueueFixture fixture = savedQueueFixture();
        Prompt prompt = promptRepository.save(Prompt.createQueued(
            fixture.queue().getId(),
            fixture.aiTool().getId(),
            "Executable prompt",
            "Run integration test",
            0,
            0,
            3,
            null
        ));

        return new PromptFixture(fixture.project(), fixture.aiTool(), fixture.queue(), prompt);
    }

    private QueueFixture savedQueueFixture() {
        Project project = savedProject("project", "/tmp/project");
        AiTool aiTool = aiToolRepository.save(AiTool.create("Fake tool", AiToolType.FAKE, "/usr/bin/fake-tool"));
        PromptQueue queue = promptQueueRepository.save(
            PromptQueue.create(project.getId(), "Default queue", QueueExecutionPolicy.defaultPolicy())
        );

        return new QueueFixture(project, aiTool, queue);
    }

    private Project savedProject(String name, String rootDirectory) {
        return projectRepository.save(Project.create(name, rootDirectory));
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting in persistence test", exception);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ProjectJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = ProjectJpaRepository.class)
    @Import({
        ProjectRepositoryAdapter.class,
        AiToolRepositoryAdapter.class,
        PromptQueueRepositoryAdapter.class,
        PromptRepositoryAdapter.class,
        PromptExecutionRepositoryAdapter.class
    })
    static class PersistenceTestConfiguration {
    }

    private record QueueFixture(Project project, AiTool aiTool, PromptQueue queue) {
    }

    private record PromptFixture(Project project, AiTool aiTool, PromptQueue queue, Prompt prompt) {
    }
}
