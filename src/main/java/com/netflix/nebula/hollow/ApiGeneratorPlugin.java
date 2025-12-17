/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.nebula.hollow;

import com.netflix.hollow.core.write.objectmapper.HollowObjectMapper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.net.URLClassLoader;
import java.util.stream.Collectors;

public class ApiGeneratorPlugin implements Plugin<Project> {

    /**
     * Task depends on build, because we need .class files for {@link URLClassLoader} to load them to
     * {@link HollowObjectMapper}
     */
    @Override
    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();

        if (plugins.hasPlugin(JavaPlugin.class)) {
            ApiGeneratorExtension extension = project.getExtensions().create("hollow", ApiGeneratorExtension.class);

            JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName("main");

            TaskProvider<JavaCompile> compileDataModelTask = project.getTasks().register("compileDataModel", JavaCompile.class, javaCompile -> {
                File destinationDir = mainSourceSet.getOutput().getClassesDirs()
                    .filter(file -> file.toString().contains("java"))
                    .getSingleFile();

                javaCompile.source(mainSourceSet.getJava().getSourceDirectories());
                // Use doFirst to lazily configure includes from the extension
                javaCompile.doFirst(task -> {
                    JavaCompile compileTask = (JavaCompile) task;
                    compileTask.include(
                        extension.getPackagesToScan().get().stream()
                            .map(pkg -> pkg.replace(".", "/") + "/**")
                            .toArray(String[]::new)
                    );
                });
                javaCompile.setClasspath(mainSourceSet.getCompileClasspath());
                javaCompile.getDestinationDirectory().set(destinationDir);
            });

            TaskProvider<ApiGeneratorTask> generateTask = project.getTasks().register("generateHollowConsumerApi", ApiGeneratorTask.class, task -> {
                task.setGroup("hollow");
                task.setDescription("Generates Hollow consumer API from data model classes");

                // Wire all extension properties to task properties
                task.getPackagesToScan().set(extension.getPackagesToScan());
                task.getApiClassName().set(extension.getApiClassName());
                task.getApiPackageName().set(extension.getApiPackageName());
                task.getGetterPrefix().set(extension.getGetterPrefix());
                task.getClassPostfix().set(extension.getClassPostfix());
                task.getDestinationPath().set(extension.getDestinationPath());
                task.getParameterizeAllClassNames().set(extension.getParameterizeAllClassNames());
                task.getUseAggressiveSubstitutions().set(extension.getUseAggressiveSubstitutions());
                task.getUseErgonomicShortcuts().set(extension.getUseErgonomicShortcuts());
                task.getUsePackageGrouping().set(extension.getUsePackageGrouping());
                task.getUseBooleanFieldErgonomics().set(extension.getUseBooleanFieldErgonomics());
                task.getReservePrimaryKeyIndexForTypeWithPrimaryKey().set(extension.getReservePrimaryKeyIndexForTypeWithPrimaryKey());
                task.getUseHollowPrimitiveTypes().set(extension.getUseHollowPrimitiveTypes());
                task.getRestrictApiToFieldType().set(extension.getRestrictApiToFieldType());
                task.getUseVerboseToString().set(extension.getUseVerboseToString());
                task.getUseGeneratedAnnotation().set(extension.getUseGeneratedAnnotation());

                // Set source directory to main java source directory
                task.getSourceDirectory().set(
                    project.provider(() -> {
                        File mainJava = mainSourceSet.getJava().getSrcDirs().stream()
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No Java source directory found"));
                        return project.getLayout().getProjectDirectory().dir(mainJava.getAbsolutePath());
                    })
                );

                // Set classpath to compiled data model classes
                task.getClasspath().from(compileDataModelTask.flatMap(javaCompile -> javaCompile.getDestinationDirectory()));

                // Set output directory based on destination path or default to source directory with API package path
                task.getOutputDirectory().set(
                    project.provider(() -> {
                        String destPath = extension.getDestinationPath().getOrElse("");
                        if (!destPath.isEmpty()) {
                            File destFile = new File(destPath);
                            if (destFile.isAbsolute()) {
                                return project.getLayout().getProjectDirectory().dir(destPath);
                            }
                            return project.getLayout().getProjectDirectory().dir(project.file(destPath).getAbsolutePath());
                        }
                        // Use a fallback directory if apiPackageName is not set (task will fail with proper validation message during execution)
                        String apiPkg = extension.getApiPackageName().getOrElse("hollow-api-fallback");
                        File mainJava = mainSourceSet.getJava().getSrcDirs().stream().findFirst().orElseThrow();
                        File outputDir = new File(mainJava, apiPkg.replace(".", "/"));
                        return project.getLayout().getProjectDirectory().dir(outputDir.getAbsolutePath());
                    })
                );

                task.dependsOn(compileDataModelTask);
            });

            TaskProvider<Delete> cleanDataModelApiTask = project.getTasks().register("cleanDataModelApi", Delete.class, deleteTask -> {
                deleteTask.delete(
                    extension.getDestinationPath().map(destPath -> {
                        if (!destPath.isEmpty()) {
                            return project.files(destPath);
                        }
                        return project.files(
                            extension.getApiPackageName().map(apiPkg -> {
                                String dataModelApiPath = apiPkg.replace(".", "/");
                                return mainSourceSet.getJava().getSrcDirs()
                                    .stream()
                                    .map(srcDir -> srcDir.toPath().resolve(dataModelApiPath).toString())
                                    .collect(Collectors.toList());
                            }).getOrElse(java.util.Collections.emptyList())
                        );
                    }).orElse(
                        project.files(
                            extension.getApiPackageName().map(apiPkg -> {
                                String dataModelApiPath = apiPkg.replace(".", "/");
                                return mainSourceSet.getJava().getSrcDirs()
                                    .stream()
                                    .map(srcDir -> srcDir.toPath().resolve(dataModelApiPath).toString())
                                    .collect(Collectors.toList());
                            }).getOrElse(java.util.Collections.emptyList())
                        )
                    )
                );
            });

            // Wire task dependencies using configuration avoidance
            project.getTasks().named("compileJava").configure(task -> task.dependsOn(generateTask));
            project.getTasks().named("clean").configure(task -> task.dependsOn(cleanDataModelApiTask));
        }
    }
}
