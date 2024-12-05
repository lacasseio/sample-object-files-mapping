package nokeesamples;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppExecutable;
import org.gradle.language.cpp.CppSharedLibrary;
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask;
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingScheme;
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory;
import org.gradle.nativeplatform.tasks.AbstractLinkTask;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/*private*/ abstract /*final*/ class ObjectFilesPlugin implements Plugin<Project> {
	@Inject
	public ObjectFilesPlugin() {}

	@Override
	public void apply(Project project) {
		project.getTasks().withType(AbstractNativeCompileTask.class).configureEach(task -> {
			final FileTree objectFiles = project.getObjects().fileCollection().builtBy(task.getObjectFileDir()).from((Callable<?>) () -> {
				final File objectFileDir = task.getObjectFileDir().getLocationOnly().get().getAsFile();
				final List<Object> result = new ArrayList<>();
				final CompilerOutputFileNamingScheme namingScheme = ((ProjectInternal) project).getServices().get(CompilerOutputFileNamingSchemeFactory.class).create().withOutputBaseFolder(objectFileDir);

				// We use both known object file suffixes as there is no way of knowing which one the toolchain will select
				//   This information is buried too deep in the compiler hierarchy.
				final CompilerOutputFileNamingScheme objNamingScheme = namingScheme.withObjectFileNameSuffix(".obj");
				final CompilerOutputFileNamingScheme oNamingScheme = namingScheme.withObjectFileNameSuffix(".o");

				// Map all source files to object files
				for (File sourceFile : task.getSource()) {
					result.add(objNamingScheme.map(sourceFile));
					result.add(oNamingScheme.map(sourceFile));
				}
				return result;
			}).getAsFileTree(); // Use FileTree to remove missing object files

			// Mount as extension
			task.getExtensions().add("objectFiles", objectFiles);
		});

		// Rewire the C++ compile/link tasks' object files
		project.getComponents().withType(CppBinary.class).configureEach(linkable(binary -> {
			project.getTasks().named(linkTaskName(binary), AbstractLinkTask.class).configure(task -> {
				task.getSource().setFrom(project.getTasks().named(compileTaskName(binary), AbstractNativeCompileTask.class).map(it -> it.getExtensions().getByName("objectFiles")));
			});
		}));
	}

	private static Action<CppBinary> linkable(Action<? super CppBinary> action) {
		return binary -> {
			if (binary instanceof CppExecutable || binary instanceof CppSharedLibrary || binary instanceof CppTestExecutable) {
				action.execute(binary);
			}
		};
	}

	//region Names
	private static String qualifyingName(CppBinary binary) {
		String result = binary.getName();
		if (result.startsWith("main")) {
			result = result.substring("main".length());
		} else if (result.endsWith("Executable")) {
			result = result.substring(0, result.length() - "Executable".length());
		}
		return uncapitalize(result);
	}

	private static String compileTaskName(CppBinary binary) {
		return "compile" + capitalize(qualifyingName(binary)) + "Cpp";
	}

	private static String linkTaskName(CppBinary binary) {
		return "link" + capitalize(qualifyingName(binary));
	}
	//endregion

	//region StringUtils
	private static String uncapitalize(String s) {
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	private static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	//endregion
}
