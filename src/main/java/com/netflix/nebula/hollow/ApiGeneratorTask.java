/*
 * Copyright 2018 Netflix, Inc.
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
    //TODO: perhaps allow the users to configure these paths?
    private final String[] compiledClassesPaths = {
            projectDirPath + "/build/classes/main/",
            projectDirPath + "/build/classes/java/main/",
    };

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

        String apiTargetPath = extension.destinationPath != null && extension.destinationPath.isEmpty() ? extension.destinationPath : buildPathToApiTargetFolder(extension.apiPackageName);

        HollowAPIGenerator generator = buildHollowAPIGenerator(extension, writeEngine, apiTargetPath);
        
        cleanupAndCreateFolders(apiTargetPath);
        generator.generateSourceFiles();
    }

    private HollowAPIGenerator buildHollowAPIGenerator(ApiGeneratorExtension extension, HollowWriteStateEngine writeStateEngine, String apiTargetPath) {
        HollowAPIGenerator.Builder builder = new HollowAPIGenerator.Builder()
                .withAPIClassname(extension.apiClassName)
                .withPackageName(extension.apiPackageName)
                .withDataModel(writeStateEngine)
                .withDestination(apiTargetPath)
                .withParameterizeAllClassNames(extension.parameterizeAllClassNames)
                .withAggressiveSubstitutions(extension.useAggressiveSubstitutions)
                .withBooleanFieldErgonomics(extension.useBooleanFieldErgonomics)
                .reservePrimaryKeyIndexForTypeWithPrimaryKey(extension.reservePrimaryKeyIndexForTypeWithPrimaryKey)
                .withHollowPrimitiveTypes(extension.useHollowPrimitiveTypes)
                .withVerboseToString(extension.useVerboseToString);

        if(extension.getterPrefix != null && !extension.getterPrefix.isEmpty()) {
            builder.withGetterPrefix(extension.getterPrefix);
        }

        if(extension.classPostfix != null && !extension.classPostfix.isEmpty()) {
            builder.withClassPostfix(extension.classPostfix);
        }

        if(extension.useErgonomicShortcuts) {
            builder.withErgonomicShortcuts();
        }

        if(extension.usePackageGrouping) {
            builder.withPackageGrouping();
        }

        if(extension.restrictApiToFieldType) {
            builder.withRestrictApiToFieldType();
        }

        return builder.build();
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
        URL[] urls = new URL[compiledClassesPaths.length];
        for (int i=0; i < compiledClassesPaths.length; i++){
            urls[i]= new File(compiledClassesPaths[i]).toURI().toURL() ;
        }
        urlClassLoader = new URLClassLoader(urls);
    }

    private void validatePluginConfiguration(ApiGeneratorExtension extension) {
        if (extension.apiClassName == null || extension.apiPackageName == null || extension.packagesToScan.isEmpty()) {
            throw new InvalidUserDataException("Specify buildscript as per plugin readme!");
        }
    }
}
