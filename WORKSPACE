load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

SPANNER_EMULATOR_VERSION = "0.8.0"

http_archive(
    name = "spanner-emulator-archive",
    build_file_content = """
exports_files(["emulator_main"])
""",
    sha256 = "19eb279c0f0a93b14796e347e6b26a27bc90b91c5578f1de1532448a37b3e3d2",
    url = "https://storage.googleapis.com/cloud-spanner-emulator/releases/" + SPANNER_EMULATOR_VERSION + "/cloud-spanner-emulator_linux_amd64-" + SPANNER_EMULATOR_VERSION + ".tar.gz",
)

RULES_JVM_EXTERNAL_TAG = "3.2"

http_archive(
    name = "rules_jvm_external",
    sha256 = "82262ff4223c5fda6fb7ff8bd63db8131b51b413d26eb49e3131037e79e324af",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)
load("@rules_jvm_external//:defs.bzl", "maven_install")
maven_install(
    artifacts = [
        "com.google.api.grpc:proto-google-common-protos:1.18.0",
        "com.google.cloud:google-cloud-core:1.93.7",
        "com.google.cloud:google-cloud-spanner:1.58.0",
    ],
    fetch_sources = True,
    maven_install_json = "//:maven_install.json",
    repositories = ["https://repo1.maven.org/maven2"],
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()
