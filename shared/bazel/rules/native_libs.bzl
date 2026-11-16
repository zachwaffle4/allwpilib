load("@rules_cc//cc/common:cc_info.bzl", "CcInfo")

_LIB_EXTENSIONS = [".so", ".dylib", ".dll"]

def _copy(ctx, src, dst):
    """Copies src to dst, using cmd.exe on Windows hosts and sh elsewhere."""
    if ctx.configuration.host_path_separator == ";":
        # Write the command to a .bat so cmd.exe's quoting rules don't matter.
        # One .bat per file, since several files are copied by one rule.
        bat = ctx.actions.declare_file(ctx.label.name + "-bat/" + dst.basename + "-copy.bat")
        ctx.actions.write(
            output = bat,
            content = "@copy /Y \"%s\" \"%s\" >NUL" % (
                src.path.replace("/", "\\"),
                dst.path.replace("/", "\\"),
            ),
            is_executable = True,
        )
        ctx.actions.run(
            inputs = [src, bat],
            outputs = [dst],
            executable = "cmd.exe",
            arguments = ["/C", bat.path.replace("/", "\\")],
            mnemonic = "StageNativeLib",
            progress_message = "Staging %s" % dst.basename,
            use_default_shell_env = True,
        )
    else:
        ctx.actions.run_shell(
            inputs = [src],
            outputs = [dst],
            command = "cp -f \"$1\" \"$2\"",
            arguments = [src.path, dst.path],
            mnemonic = "StageNativeLib",
            progress_message = "Staging %s" % dst.basename,
            use_default_shell_env = True,
        )

def _is_shared_lib(f):
    for ext in _LIB_EXTENSIONS:
        if f.basename.endswith(ext):
            return True
    return False

def _native_lib_dir_impl(ctx):
    # Collect the libraries themselves plus whatever they need at runtime
    # (cc_shared_library puts its dynamic_deps in the runfiles).
    libs = {}
    for target in ctx.attr.libs:
        files = target[DefaultInfo].files.to_list() + target[DefaultInfo].default_runfiles.files.to_list()
        for f in files:
            if _is_shared_lib(f):
                libs[f.basename] = f

        # cc_library wrappers around prebuilt libraries (e.g. OpenCV) expose
        # their shared libraries only through CcInfo. No extension filter
        # here: versioned names like libfoo.so.413 are legitimate.
        if CcInfo in target:
            for linker_input in target[CcInfo].linking_context.linker_inputs.to_list():
                for lib in linker_input.libraries:
                    f = lib.resolved_symlink_dynamic_library or lib.dynamic_library
                    if f:
                        libs[f.basename] = f

    # Flatten everything into <package>/<name>/ so a single java.library.path
    # entry resolves every library.
    staged = []
    for basename, src in libs.items():
        dst = ctx.actions.declare_file(ctx.label.name + "/" + basename)
        _copy(ctx, src, dst)
        staged.append(dst)

    return [DefaultInfo(
        files = depset(staged),
        runfiles = ctx.runfiles(files = staged),
    )]

wpilib_native_lib_dir = rule(
    implementation = _native_lib_dir_impl,
    attrs = {
        "libs": attr.label_list(
            doc = "cc_shared_library targets to stage (their runfiles are staged too).",
            mandatory = True,
        ),
    },
    doc = """Copies shared libraries into <package>/<name>/, a single directory suitable for
-Djava.library.path.""",
)
