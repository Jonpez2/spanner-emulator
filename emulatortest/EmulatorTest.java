package emulatortest;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfig;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Struct;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class EmulatorTest {
    private static Process makeTheSpanner() throws IOException {
        Path emulator = Paths.get(System.getProperty("PATH_TO_EMULATOR"));
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(emulator);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(emulator, perms);

        return new ProcessBuilder()
            .command(emulator.toAbsolutePath().toString())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    }

    public static class TestInstance implements Closeable {
        private final SpannerOptions options;
        private final Process spannerProc;
        private final DatabaseClient dbClient;

        private static final String database = "test-db";

        public TestInstance() {
            try {
                this.spannerProc = makeTheSpanner();
                System.out.println("Spanner proc - " + spannerProc.pid());
                this.options = SpannerOptions.newBuilder()
                    .setProjectId("NO_PROJECT_FOR_TEST")
                    .setEmulatorHost("localhost:10007").build();

                InstanceId instanceId = InstanceId.of(options.getProjectId(), "test-instance");
                InstanceAdminClient instanceAdminClient = options.getService().getInstanceAdminClient();
    
                InstanceConfig instanceConfig =
                    instanceAdminClient.listInstanceConfigs().iterateAll().iterator().next();
        
                InstanceConfigId configId = instanceConfig.getId();
                System.out.println("Creating instance using config " + configId);
                InstanceInfo instanceInfo =
                    InstanceInfo.newBuilder(instanceId)
                        .setNodeCount(1)
                        .setDisplayName("Test instance")
                        .setInstanceConfigId(configId)
                        .build();
    
                Instance created = instanceAdminClient.createInstance(instanceInfo)
                    .get(30000L, TimeUnit.MILLISECONDS);

                DatabaseAdminClient dbAdminClient = options.getService().getDatabaseAdminClient();
                Database db;
                try {
                        db = dbAdminClient.createDatabase(
                            created.getId().getInstance(),
                            database,
                            Arrays.asList("CREATE TABLE Test ( AString STRING(255) ) PRIMARY KEY (AString)")
                    ).get();
                } catch (ExecutionException e) {
                    // If the operation failed during execution, expose the cause.
                    throw (SpannerException) e.getCause();
                } catch (InterruptedException e) {
                    // Throw when a thread is waiting, sleeping, or otherwise occupied,
                    // and the thread is interrupted, either before or during the activity.
                    throw SpannerExceptionFactory.propagateInterrupt(e);
                }

                dbClient = options.getService().getDatabaseClient(db.getId());
            } catch( Exception e ) {
                close();
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            try {
                if(spannerProc != null)
                    spannerProc.destroyForcibly().waitFor();
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt(); 
                throw new RuntimeException(e);
            }
        }
    }
    
    // @Test
    public void doTest() throws Exception {
        try( TestInstance instance = new TestInstance()) {
            Mutation mut = Mutation.newInsertBuilder("Test").set("AString").to("Hello").build();
            instance.dbClient.readWriteTransaction()
                .run(t -> {
                    t.buffer(mut);
                    return null;
                });

            Struct row = instance.dbClient.singleUseReadOnlyTransaction().readRow("Test", Key.of("Hello"), Arrays.asList("AString"));
            System.out.println(row);
        }
    }

    public static void main(String[] args) throws Exception {
        new EmulatorTest().doTest();
    }
}