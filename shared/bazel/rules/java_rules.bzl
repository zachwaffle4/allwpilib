load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_test")
load("//shared/bazel/rules:native_libs.bzl", "wpilib_native_lib_dir")
load("//shared/bazel/rules:packaging.bzl", "zip_java_srcs")
load("//shared/bazel/rules:publishing.bzl", "wpilib_maven_export")

def patch_module(module_name, patch_dirs):
    """
    Returns a platform-aware --patch-module javacopt.
    """
    patch_str = ":".join(patch_dirs)
    patch_str_win = ";".join([d.replace("/", "\\\\") for d in patch_dirs])

    return select({
        "@bazel_tools//src/conditions:windows": ["--patch-module={}={}".format(module_name, patch_str_win)],
        "//conditions:default": ["--patch-module={}={}".format(module_name, patch_str)],
    })

def wpilib_java_library(
        name,
        maven_group_id,
        maven_artifact_name,
        tags = [],
        extra_source_pkgs = [],
        java_srcs_root = "",
        generated_java_srcs = None,
        resource_srcs = None,
        proto_srcs = None,
        **kwargs):
    tags = list(tags) if tags else []

    maven_coordinates = "{}:{}:$(WPILIB_VERSION)".format(maven_group_id, maven_artifact_name)
    tags.append("maven_coordinates=" + maven_coordinates)

    java_library(
        name = name,
        tags = tags,
        **kwargs
    )

    zip_java_srcs(
        name = name,
        extra_pkgs = extra_source_pkgs,
        java_srcs_root = java_srcs_root,
        generated_java_srcs = generated_java_srcs,
        resource_srcs = resource_srcs,
        proto_srcs = proto_srcs,
    )

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
        args = [],
        tags = [],
        package = "org.wpilib",
        native_libs = [],
        jvm_flags = [],
        **kwargs):
    """
    Convenience helper to make a junit5 test

    Args:
      native_libs: cc_shared_library targets (typically JNI libraries) the test
                   needs at runtime. They are staged into a single directory
                   that is put on java.library.path.
    """
    data = list(kwargs.pop("data", []))
    jvm_flags = list(jvm_flags)

    if native_libs:
        natives_name = name + "_natives"
        wpilib_native_lib_dir(
            name = natives_name,
            libs = native_libs,
            testonly = True,
        )
        data.append(":" + natives_name)

        # Tests run with the main repo's runfiles directory as their working
        # directory on every platform, so a package-relative path works
        # without relying on shell-only variables like JAVA_RUNFILES.
        jvm_flags.append("-Djava.library.path=" + native.package_name() + "/" + natives_name)

    junit_deps = [
        "@maven//:org_junit_jupiter_junit_jupiter_api",
        "@maven//:org_junit_jupiter_junit_jupiter_params",
        "@maven//:org_junit_jupiter_junit_jupiter_engine",
    ]

    junit_runtime_deps = [
        "@maven//:org_junit_platform_junit_platform_console",
    ]

    # TODO - replace with java_test once shared libraries are hooked up.
    java_test(
        name = name,
        deps = deps + junit_deps,
        runtime_deps = runtime_deps + junit_runtime_deps,
        args = args + ["--select-package", package],
        data = data,
        jvm_flags = jvm_flags,
        main_class = "org.junit.platform.console.ConsoleLauncher",
        use_testrunner = False,
        testonly = True,
        tags = tags + ["allwpilib-build-java", "no-asan", "no-tsan", "no-ubsan"],
        **kwargs
    )
