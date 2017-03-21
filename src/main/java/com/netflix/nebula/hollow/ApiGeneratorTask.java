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

import com.netflix.hollow.api.codegen.HollowAPIGenerator;
import com.netflix.hollow.core.write.HollowWriteStateEngine;
import com.netflix.hollow.core.write.objectmapper.HollowObjectMapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ApiGeneratorTask extends DefaultTask {

    private final File projectDirFile = getProject().getProjectDir();
    private final String projectDirPath = projectDirFile.getAbsolutePath();
    private final String relativeJavaSourcesPath = "/src/main/java/";
    private final String javaSourcesPath = projectDirPath + relativeJavaSourcesPath;
    private final String compiledClassesPath = projectDirPath + "/build/classes/main/";

    private URLClassLoader urlClassLoader;

    @TaskAction
    public void generateApi() throws IOException {
        ApiGeneratorExtension extension = getProject().getExtensions().getByType(ApiGeneratorExtension.class);
        validatePluginConfiguration(extension);

        initClassLoader();

        HollowWriteStateEngine writeEngine = new HollowWriteStateEngine();
        HollowObjectMapper mapper = new HollowObjectMapper(writeEngine);

        Collection<Class<?>> datamodelClasses = extractClasses(extension.packagesToScan);
        for (Class<?> clazz : datamodelClasses) {
            getLogger().debug("Initialize schema for class {}", clazz.getName());
            mapper.initializeTypeState(clazz);
        }

        HollowAPIGenerator generator =
                new HollowAPIGenerator(
                        extension.apiClassName,
                        extension.apiPackageName,
                        writeEngine
                );

        String apiTargetPath = buildPathToApiTargetFolder(extension.apiPackageName);

        cleanupAndCreateFolders(apiTargetPath);
        generator.generateFiles(apiTargetPath);
    }

    private Collection<Class<?>> extractClasses(List<String> packagesToScan) {
        Set<Class<?>> classes = new HashSet<>();

        for (String packageToScan : packagesToScan) {
            File packageFile = buildPackageFile(packageToScan);

            List<File> allFilesInPackage = findFilesRecursively(packageFile);
            List<String> classNames = new ArrayList<>();
            for (File file : allFilesInPackage) {
                String filePath = file.getAbsolutePath();
                getLogger().debug("Candidate for schema initialization {}", filePath);
                if (filePath.endsWith(".java") &&
                        !filePath.endsWith("package-info.java") &&
                        !filePath.endsWith("module-info.java")
                        ) {
                    String relativeFilePath = removeSubstrings(filePath, projectDirPath, relativeJavaSourcesPath);
                    classNames.add(convertFolderPathToPackageName(removeSubstrings(relativeFilePath, ".java")));
                }
            }

            for (String fqdn : classNames) {
                try {
                    Class<?> clazz = urlClassLoader.loadClass(fqdn);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    getLogger().warn("{} class not found", fqdn);
                }
            }
        }
        return classes;
    }

    private List<File> findFilesRecursively(File packageFile) {
        List<File> foundFiles = new ArrayList<>();
        if (packageFile.exists()) {
            for (File file : packageFile.listFiles()) {
                if (file.isDirectory()) {
                    foundFiles.addAll(findFilesRecursively(file));
                } else {
                    foundFiles.add(file);
                }
            }
        }
        return foundFiles;
    }

    private File buildPackageFile(String packageName) {
        return new File(javaSourcesPath + convertPackageNameToFolderPath(packageName));
    }

    private String buildPathToApiTargetFolder(String apiPackageName) {
        return javaSourcesPath + convertPackageNameToFolderPath(apiPackageName);
    }

    private String convertPackageNameToFolderPath(String packageName) {
        return packageName.replaceAll("\\.", "/");
    }

    private String convertFolderPathToPackageName(String folderName) {
        return folderName.replaceAll("/", "\\.");
    }

    private String removeSubstrings(String result, String... substrings) {
        for (String substring : substrings) {
            result = result.replace(substring, "");
        }
        return result;
    }

    private void cleanupAndCreateFolders(String generatedApiTarget) {
        File apiCodeFolder = new File(generatedApiTarget);
        apiCodeFolder.mkdirs();
        for (File f : apiCodeFolder.listFiles()) {
            f.delete();
        }
    }

    private void initClassLoader() throws MalformedURLException {
        URL url = new File(compiledClassesPath).toURI().toURL();
        URL[] urls = new URL[] { url };
        urlClassLoader = new URLClassLoader(urls);
    }

    private void validatePluginConfiguration(ApiGeneratorExtension extension) {
        if (extension.apiClassName == null || extension.apiPackageName == null || extension.packagesToScan.isEmpty()) {
            throw new InvalidUserDataException("Specify buildscript as per plugin readme!");
        }
    }
}
