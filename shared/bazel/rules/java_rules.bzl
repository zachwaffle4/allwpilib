load("@bazel_skylib//lib:paths.bzl", "paths")
load("@rules_java//java:defs.bzl", "java_library", "java_test")
load("//shared/bazel/rules:packaging.bzl", "zip_java_srcs")
load("//shared/bazel/rules:publishing.bzl", "wpilib_maven_export")

def wpilib_java_library(
        name,
        maven_group_id,
        maven_artifact_name,
        tags = [],
        extra_source_pkgs = [],
        **kwargs):
    tags = list(tags) if tags else []

    maven_coordinates = "{}:{}:$(WPILIB_VERSION)".format(maven_group_id, maven_artifact_name)
    tags.append("maven_coordinates=" + maven_coordinates)

    java_library(
        name = name,
        tags = tags,
        **kwargs
    )

    zip_java_srcs(name = name, extra_pkgs = extra_source_pkgs)

    wpilib_maven_export(
        name = "{}_publish".format(name),
        classifier_artifacts = {"sources": ":lib{}-sources.jar".format(name)},
        lib_name = name,
        maven_coordinates = maven_coordinates,
        visibility = ["//visibility:public"],
    )

def wpilib_java_junit5_test(
        name,
        deps = [],
        runtime_deps = [],
        native_libs = [],
        data = [],
        jvm_flags = [],
        args = [],
        tags = [],
        package = "org",
        **kwargs):
    """
    Convenience helper to make a junit5 test

    native_libs is a list of native shared library targets (e.g. the
    wpilib_cc_shared_library JNI target for a library under test) to put on
    java.library.path so RuntimeLoader.loadLibrary can find them.

    cc_shared_library doesn't expose the CcInfo that java_test's automatic
    java.library.path detection relies on, so the directories are computed
    here from the labels and passed via an explicit jvm_flag instead. This
    relies on ${JAVA_RUNFILES} being set by the java stub script and the main
    repo's runfiles directory being named "_main", matching what java_test
    itself would generate for a CcInfo-bearing runtime dep.
    """
    junit_deps = [
        "@maven//:org_junit_jupiter_junit_jupiter_api",
        "@maven//:org_junit_jupiter_junit_jupiter_params",
        "@maven//:org_junit_jupiter_junit_jupiter_engine",
    ]

    junit_runtime_deps = [
        "@maven//:org_junit_platform_junit_platform_console",
    ]

    native_lib_dirs = []
    for lib in native_libs:
        label = native.package_relative_label(lib)
        subdir = paths.dirname(label.name)
        lib_dir = label.package + "/" + subdir if subdir else label.package
        if lib_dir not in native_lib_dirs:
            native_lib_dirs.append(lib_dir)

    if native_lib_dirs:
        jvm_flags = jvm_flags + [
            "-Djava.library.path=" + ":".join(
                ["$${{JAVA_RUNFILES}}/_main/{}".format(d) for d in native_lib_dirs],
            ),
        ]

    java_test(
        name = name,
        deps = deps + junit_deps,
        data = data + native_libs,
        runtime_deps = runtime_deps + junit_runtime_deps,
        jvm_flags = jvm_flags + [
            "-ea",
            "--enable-native-access=ALL-UNNAMED",
        ],
        args = args + ["--select-package", package],
        main_class = "org.junit.platform.console.ConsoleLauncher",
        use_testrunner = False,
        testonly = True,
        tags = tags + ["allwpilib-build-java", "no-asan", "no-tsan", "no-ubsan"],
        **kwargs
    )
