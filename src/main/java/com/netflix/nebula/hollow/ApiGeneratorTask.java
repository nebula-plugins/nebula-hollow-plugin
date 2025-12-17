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
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@CacheableTask
public class ApiGeneratorTask extends DefaultTask {

    private final ListProperty<String> packagesToScan;
    private final Property<String> apiClassName;
    private final Property<String> apiPackageName;
    private final Property<String> getterPrefix;
    private final Property<String> classPostfix;
    private final Property<String> destinationPath;
    private final Property<Boolean> parameterizeAllClassNames;
    private final Property<Boolean> useAggressiveSubstitutions;
    private final Property<Boolean> useErgonomicShortcuts;
    private final Property<Boolean> usePackageGrouping;
    private final Property<Boolean> useBooleanFieldErgonomics;
    private final Property<Boolean> reservePrimaryKeyIndexForTypeWithPrimaryKey;
    private final Property<Boolean> useHollowPrimitiveTypes;
    private final Property<Boolean> restrictApiToFieldType;
    private final Property<Boolean> useVerboseToString;
    private final Property<Boolean> useGeneratedAnnotation;
    private final DirectoryProperty sourceDirectory;
    private final ConfigurableFileCollection classpath;
    private final DirectoryProperty outputDirectory;

    private URLClassLoader urlClassLoader;

    @Inject
    public ApiGeneratorTask(ObjectFactory objects) {
        this.packagesToScan = objects.listProperty(String.class);
        this.apiClassName = objects.property(String.class);
        this.apiPackageName = objects.property(String.class);
        this.getterPrefix = objects.property(String.class);
        this.classPostfix = objects.property(String.class);
        this.destinationPath = objects.property(String.class);
        this.parameterizeAllClassNames = objects.property(Boolean.class);
        this.useAggressiveSubstitutions = objects.property(Boolean.class);
        this.useErgonomicShortcuts = objects.property(Boolean.class);
        this.usePackageGrouping = objects.property(Boolean.class);
        this.useBooleanFieldErgonomics = objects.property(Boolean.class);
        this.reservePrimaryKeyIndexForTypeWithPrimaryKey = objects.property(Boolean.class);
        this.useHollowPrimitiveTypes = objects.property(Boolean.class);
        this.restrictApiToFieldType = objects.property(Boolean.class);
        this.useVerboseToString = objects.property(Boolean.class);
        this.useGeneratedAnnotation = objects.property(Boolean.class);
        this.sourceDirectory = objects.directoryProperty();
        this.classpath = objects.fileCollection();
        this.outputDirectory = objects.directoryProperty();
    }

    @Optional
    @Input
    public ListProperty<String> getPackagesToScan() {
        return packagesToScan;
    }

    @Optional
    @Input
    public Property<String> getApiClassName() {
        return apiClassName;
    }

    @Optional
    @Input
    public Property<String> getApiPackageName() {
        return apiPackageName;
    }

    @Optional
    @Input
    public Property<String> getGetterPrefix() {
        return getterPrefix;
    }

    @Optional
    @Input
    public Property<String> getClassPostfix() {
        return classPostfix;
    }

    @Optional
    @Input
    public Property<String> getDestinationPath() {
        return destinationPath;
    }

    @Input
    public Property<Boolean> getParameterizeAllClassNames() {
        return parameterizeAllClassNames;
    }

    @Input
    public Property<Boolean> getUseAggressiveSubstitutions() {
        return useAggressiveSubstitutions;
    }

    @Input
    public Property<Boolean> getUseErgonomicShortcuts() {
        return useErgonomicShortcuts;
    }

    @Input
    public Property<Boolean> getUsePackageGrouping() {
        return usePackageGrouping;
    }

    @Input
    public Property<Boolean> getUseBooleanFieldErgonomics() {
        return useBooleanFieldErgonomics;
    }

    @Input
    public Property<Boolean> getReservePrimaryKeyIndexForTypeWithPrimaryKey() {
        return reservePrimaryKeyIndexForTypeWithPrimaryKey;
    }

    @Input
    public Property<Boolean> getUseHollowPrimitiveTypes() {
        return useHollowPrimitiveTypes;
    }

    @Input
    public Property<Boolean> getRestrictApiToFieldType() {
        return restrictApiToFieldType;
    }

    @Input
    public Property<Boolean> getUseVerboseToString() {
        return useVerboseToString;
    }

    @Input
    public Property<Boolean> getUseGeneratedAnnotation() {
        return useGeneratedAnnotation;
    }

    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public DirectoryProperty getSourceDirectory() {
        return sourceDirectory;
    }

    @Classpath
    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }

    @OutputDirectory
    public DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

    @TaskAction
    public void generateApi() throws IOException {
        // Validate required configuration
        if (!apiClassName.isPresent() || !apiPackageName.isPresent() || !packagesToScan.isPresent() || packagesToScan.get().isEmpty()) {
            throw new InvalidUserDataException(
                "Specify buildscript as per plugin readme | apiClassName, apiPackageName and packagesToScan configuration values must be present"
            );
        }

        initClassLoader();

        HollowWriteStateEngine writeEngine = new HollowWriteStateEngine();
        HollowObjectMapper mapper = new HollowObjectMapper(writeEngine);

        Collection<Class<?>> datamodelClasses = extractClasses(packagesToScan.get());
        for (Class<?> clazz : datamodelClasses) {
            getLogger().debug("Initialize schema for class {}", clazz.getName());
            mapper.initializeTypeState(clazz);
        }

        String apiTargetPath = destinationPath.isPresent() && !destinationPath.get().isEmpty()
            ? destinationPath.get()
            : buildPathToApiTargetFolder(apiPackageName.get());

        HollowAPIGenerator generator = buildHollowAPIGenerator(writeEngine, apiTargetPath);

        cleanupAndCreateFolders(apiTargetPath);
        generator.generateSourceFiles();
    }

    private HollowAPIGenerator buildHollowAPIGenerator(HollowWriteStateEngine writeStateEngine, String apiTargetPath) {
        HollowAPIGenerator.Builder builder = new HollowAPIGenerator.Builder()
                .withAPIClassname(apiClassName.get())
                .withPackageName(apiPackageName.get())
                .withDataModel(writeStateEngine)
                .withDestination(apiTargetPath)
                .withParameterizeAllClassNames(parameterizeAllClassNames.get())
                .withAggressiveSubstitutions(useAggressiveSubstitutions.get())
                .withBooleanFieldErgonomics(useBooleanFieldErgonomics.get())
                .reservePrimaryKeyIndexForTypeWithPrimaryKey(reservePrimaryKeyIndexForTypeWithPrimaryKey.get())
                .withHollowPrimitiveTypes(useHollowPrimitiveTypes.get())
                .withVerboseToString(useVerboseToString.get());
        if (useGeneratedAnnotation.get()) {
            builder.withGeneratedAnnotation();
        }

        if(getterPrefix.isPresent() && !getterPrefix.get().isEmpty()) {
            builder.withGetterPrefix(getterPrefix.get());
        }

        if(classPostfix.isPresent() && !classPostfix.get().isEmpty()) {
            builder.withClassPostfix(classPostfix.get());
        }

        if(useErgonomicShortcuts.get()) {
            builder.withErgonomicShortcuts();
        }

        if(usePackageGrouping.get()) {
            builder.withPackageGrouping();
        }

        if(restrictApiToFieldType.get()) {
            builder.withRestrictApiToFieldType();
        }

        return builder.build();
    }

    private Collection<Class<?>> extractClasses(List<String> packagesToScan) {
        Set<Class<?>> classes = new HashSet<>();
        File sourceDirFile = sourceDirectory.get().getAsFile();
        String sourceDirPath = sourceDirFile.getAbsolutePath();

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
                    String relativeFilePath = filePath.substring(sourceDirPath.length() + 1);
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
        return new File(sourceDirectory.get().getAsFile(), convertPackageNameToFolderPath(packageName));
    }

    private String buildPathToApiTargetFolder(String apiPackageName) {
        return new File(sourceDirectory.get().getAsFile(), convertPackageNameToFolderPath(apiPackageName)).getAbsolutePath();
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
        List<URL> urls = new ArrayList<>();
        for (File file : classpath.getFiles()) {
            urls.add(file.toURI().toURL());
        }
        urlClassLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }
}
