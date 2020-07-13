package emulatortest;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.*;
import com.google.cloud.spanner.Instance;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceRequest;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;
// import org.junit.Test;


public class EmulatorTest {
    private static Closeable startEmulator(Path emulator) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(emulator);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(emulator, perms);

        Process proc = new ProcessBuilder()
            .command(emulator.toAbsolutePath().toString())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();

        return new Closeable() {
            public void close() {
                proc.destroy();
            }
        };
    }

    // @Test
    public void doTest() throws Exception {
        try( Closeable emulator = startEmulator(Paths.get(System.getProperty("PATH_TO_EMULATOR")))) {
            SpannerOptions options = SpannerOptions.newBuilder().setEmulatorHost(System.getenv("SPANNER_EMULATOR_HOST")).build();

            InstanceId instanceId = InstanceId.of(options.getProjectId(), "test-instance");
            InstanceAdminClient instanceAdminClient = options.getService().getInstanceAdminClient();

            // Nothing returned here, so I have to create an instance
            for( Instance i : instanceAdminClient.listInstances().iterateAll())  {
                System.out.println(i.getDisplayName());
            }

            // I guess I have to find a config?
            // Nope, this goes boom accessing a real project in GCP.
            InstanceConfig instanceConfig =
                instanceAdminClient.listInstanceConfigs().iterateAll().iterator().next();
    
            InstanceConfigId configId = instanceConfig.getId();
            System.out.println("Creating instance using config " + configId);
            InstanceInfo instance =
                InstanceInfo.newBuilder(instanceId)
                    .setNodeCount(1)
                    .setDisplayName("Test instance")
                    .setInstanceConfigId(configId)
                    .build();

            OperationFuture<Instance, CreateInstanceMetadata> op =
                instanceAdminClient.createInstance(instance);
            Instance createdInstance;
            try {
              createdInstance = op.get(30000L, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
              throw SpannerExceptionFactory.newSpannerException(e);
            }
            Thread.sleep(20_000);       
        }
    }

    public static SpannerOptions spannerOptions() {
        return SpannerOptions.newBuilder().setProjectId("sky-did-82840")
                .setSessionPoolOption(SessionPoolOptions.newBuilder()
                        .build())
                .build();
    }

    public static void main(String[] args) throws Exception {
        new EmulatorTest().doTest();
    }
}