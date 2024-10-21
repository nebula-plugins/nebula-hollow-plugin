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
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            Map<String, Object> taskPropertiesMap = new HashMap<>();

            taskPropertiesMap.put("name", "generateHollowConsumerApi");
            taskPropertiesMap.put("group", "hollow");
            taskPropertiesMap.put("type", ApiGeneratorTask.class);

            Task generateTask = project.getTasks().create(taskPropertiesMap);
            ApiGeneratorExtension extension = project.getExtensions().create("hollow", ApiGeneratorExtension.class);

            JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName("main");

            project.getTasks().register("compileDataModel", JavaCompile.class, javaCompile -> {
                List<String> packages = extension.packagesToScan
                    .stream()
                    .map(pkg -> pkg.replace(".", "/") + "/**")
                    .collect(Collectors.toList());

                File destinationDir = mainSourceSet.getOutput().getClassesDirs()
                    .filter(file -> file.toString().contains("java"))
                    .getSingleFile();

                javaCompile.setSource(mainSourceSet.getJava().getSrcDirs());
                javaCompile.include(packages);
                javaCompile.setClasspath(mainSourceSet.getCompileClasspath());
                javaCompile.getDestinationDirectory().set(destinationDir);
            });

            project.getTasks().register("cleanDataModelApi", Delete.class, deleteTask -> {
                if (!extension.destinationPath.isEmpty()) {
                    deleteTask.delete(extension.destinationPath);
                    return;
                }

                String dataModelApiPath = extension.apiPackageName.replace(".", "/");

                List<String> paths = mainSourceSet.getJava().getSrcDirs()
                    .stream()
                    .map(srcDir -> srcDir.toPath().resolve(dataModelApiPath).toString())
                    .collect(Collectors.toList());

                deleteTask.delete(paths);
            });

            generateTask.dependsOn("compileDataModel");
            project.getTasks().getByName("compileJava").dependsOn(generateTask);
            project.getTasks().getByName("clean").dependsOn("cleanDataModelApi");
        }
    }
}
