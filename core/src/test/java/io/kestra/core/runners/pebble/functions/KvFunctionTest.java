package io.kestra.core.runners.pebble.functions;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.executions.LogEntry;
import io.kestra.core.models.executions.TaskRun;
import io.kestra.core.models.flows.State;
import io.kestra.core.queues.QueueFactoryInterface;
import io.kestra.core.queues.QueueInterface;
import io.kestra.core.runners.AbstractMemoryRunnerTest;
import io.kestra.core.runners.RunnerUtils;
import io.kestra.core.secret.SecretService;
import io.kestra.core.storages.StorageContext;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.storages.kv.InternalKVStore;
import io.kestra.core.storages.kv.KVStore;
import io.kestra.core.storages.kv.KVStoreValueWrapper;
import io.kestra.core.utils.TestsUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
public class KvFunctionTest extends AbstractMemoryRunnerTest {
    @Inject
    private RunnerUtils runnerUtils;

    @Inject
    private StorageInterface storageInterface;

    @Inject
    private QueueInterface<LogEntry> logQueue;

    @BeforeEach
    void reset() throws IOException {
        storageInterface.deleteByPrefix(null, URI.create(StorageContext.kvPrefix("io.kestra.tests")));
    }

    @Test
    void get() throws TimeoutException, IOException {
        KVStore kv = new InternalKVStore(null, "io.kestra.tests", storageInterface);
        kv.put("my-key", new KVStoreValueWrapper<>(null, Map.of("field", "value")));

        Execution execution = runnerUtils.runOne(null, "io.kestra.tests", "kv");
        assertThat(execution.getTaskRunList().getFirst().getOutputs().get("value"), is("value"));
        assertThat(execution.getTaskRunList().get(1).getOutputs().get("value"), is("value"));
    }

    @Test
    void getAnotherNamespace() throws TimeoutException, IOException {
        KVStore kv = new InternalKVStore(null, "io.kestra.tests", storageInterface);
        kv.put("my-key", new KVStoreValueWrapper<>(null, Map.of("field", "value")));
        KVStore kvForOtherNs = new InternalKVStore(null, "io.kestra.another.ns", storageInterface);
        kvForOtherNs.put("my-key", new KVStoreValueWrapper<>(null, Map.of("field", "anotherValue")));

        Execution execution = runnerUtils.runOne(null, "io.kestra.tests", "kv", null, (flow, exec) -> Map.of("namespace", "io.kestra.another.ns"));
        assertThat(execution.getTaskRunList().getFirst().getOutputs().get("value"), is("value"));
        assertThat(execution.getTaskRunList().get(1).getOutputs().get("value"), is("anotherValue"));
    }

    @Test
    void getKeyNotFound() throws TimeoutException {
        Flux<LogEntry> receive = TestsUtils.receive(logQueue);

        Execution execution = runnerUtils.runOne(null, "io.kestra.tests", "kv", null, (flow, exec) -> Map.of("errorOnMissing", true));
        assertThat(execution.getTaskRunList().getFirst().getOutputs().get("value"), is(""));
        assertThat(execution.getTaskRunList().getFirst().getState().getCurrent(), is(State.Type.SUCCESS));
        TaskRun taskRun = execution.getTaskRunList().get(1);
        assertThat(taskRun.getState().getCurrent(), is(State.Type.FAILED));

        assertThat(
            receive.toStream()
                .filter(logEntry -> logEntry.getTaskRunId() != null && logEntry.getTaskRunId().equals(taskRun.getId()))
                .anyMatch(log -> log.getMessage().contains("io.pebbletemplates.pebble.error.PebbleException: The key 'my-key' does not exist in the namespace 'io.kestra.tests'. ({{ kv('my-key', inputs.namespace, inputs.errorOnMissing).field }}:1")),
            is(true)
        );
    }
}
