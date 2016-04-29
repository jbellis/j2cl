"""j2cl_transpile build rule

Takes Java source and translates it into Closure style JS in a zip bundle. Java
library deps might be needed for reference resolution.


Example use:

j2cl_transpile(
    name = "my_transpile",
    srcs = ["MyJavaFile.java"],
    deps = [":some_dep"],
)

Note: in general you want to be using j2cl_library instead of using
j2cl_transpile directly.

"""


def _should_omit(java_file, omit_srcs):
  for _omit_src in omit_srcs:
    if java_file.path.endswith(_omit_src):
      return True
  return False

def _get_message(ctx):
  _MESSAGES = [
    "Re" + "ti" + "cu" + "la" + "ti" + "ng" + "Sp" + "li" + "ne" + "s",
    3 * ("\\" + "0" + "/ "),
    "Co" + "mp" + "ut" + "in" + "g " + "PI",
    "So" + " m" + "uch" + " Ja" + "va",
    "Ma" + "ki" + "ng"  + " c" + "of" + "fee",
    "So" + "lv" + "in" + "g " + "ha" + "lt" + "ing" + " p" + "ro" + "bl" + "em",
    "Ex" + "ec" + "ut" + "in" + "g " + "bu" + "sy" + " l" + "oo" + "p",
    "En" + "te" + "ri" + "ng" + " w" + "ar" + "p " + "sp" + "ee" + "d"
  ]
  index = len(ctx.attr.srcs) + len(ctx.configuration.bin_dir.path)
  return _MESSAGES[index % len(_MESSAGES)] + " %s" % ctx

def _impl(ctx):
  separator = ctx.configuration.host_path_separator
  java_files = ctx.files.srcs  # java files that need to be compiled
  omit_java_files = ctx.attr.omit_srcs  # java files whose js to ignore
  js_native_zip_files = ctx.files.native_srcs_zips
  deps = ctx.attr.deps
  dep_files = set()
  deps_paths = []
  java_files_paths = []
  omit_java_files_paths = []
  js_files = []

  # base package for the build
  package_name = ctx.label.package

  # gather transitive files and exported files in deps
  for dep in deps:
    dep_files += dep.files
    dep_files += dep.default_runfiles.files  # for exported libraries

  # convert files to paths
  for dep_file in dep_files:
    deps_paths += [dep_file.path]

  for java_file in java_files:
    if _should_omit(java_file, omit_java_files):
      omit_java_files_paths += [java_file.path]
    java_files_paths += [java_file.path]

  js_zip_name = ctx.label.name + ".js.zip"
  js_zip_artifact = ctx.new_file(js_zip_name)

  compiler_args = [
      "-d",
      ctx.configuration.bin_dir.path + "/" + ctx.label.package + "/" +
      js_zip_name
  ]

  if len(deps_paths) > 0:
    compiler_args += ["-cp", separator.join(deps_paths)]

  if len(omit_java_files_paths) > 0:
    compiler_args += ["-omitfiles", separator.join(omit_java_files_paths)]

  # Add the native zip file paths
  js_native_zip_files_paths = [js_native_zip_file.path for js_native_zip_file
                               in js_native_zip_files]
  if js_native_zip_files_paths:
    joined_paths = separator.join(js_native_zip_files_paths)
    compiler_args += ["-nativesourcezip", joined_paths]

  # Generate readable_maps
  if ctx.attr.readable_source_maps:
    compiler_args += ["-readableSourceMaps"]

  # Emit goog.module.declareLegacyNamespace(). This is a temporary measure
  # while onboarding Docs, do not use.
  if ctx.attr.declare_legacy_namespace:
    compiler_args += ["-declareLegacyNamespace"]

  # The transpiler expects each java file path as a separate argument.
  compiler_args += java_files_paths

  ctx.action(
      progress_message = _get_message(ctx),
      inputs=java_files + list(dep_files) + js_native_zip_files,
      outputs=[js_zip_artifact],
      executable=ctx.executable.transpiler,
      arguments=compiler_args,
      env=dict(LANG="en_US.UTF-8"),
  )

  return struct(
      files=set([js_zip_artifact]),
  )


"""j2cl_transpile: A J2CL transpile rule.

Args:
  srcs: Source files (.java or .srcjar) to compile.
  deps: Java jar files for reference resolution.
  native_srcs_zips: JS zip files providing Foo.native.js implementations.
"""
# Private Args:
#   omit_srcs: Names of files to omit from the generated output. The files
#       will be included in the compile for reference resolution purposes but
#       no output JS for them will be kept. If used it will also disable APT
#       support since it's not possible to filter the Java files contained
#       within the created srcjar.
#   transpiler: J2CL compiler jar to use.
j2cl_transpile = rule(
    attrs={
        "deps": attr.label_list(allow_files=FileType([".jar"])),
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=FileType([".java", ".srcjar"]),
        ),
        "native_srcs_zips": attr.label_list(
            allow_files=FileType([".zip"]),
        ),
        "omit_srcs": attr.string_list(default=[]),
        "readable_source_maps": attr.bool(default=False),
        "declare_legacy_namespace": attr.bool(default=False),
        "transpiler": attr.label(
            cfg=HOST_CFG,
            executable=True,
            allow_files=True,
            default=Label("//third_party/java/j2cl:J2clTranspiler"),
        ),
    },
    implementation=_impl,
    outputs={
      "files": "%{name}.js.zip"
    }
)
