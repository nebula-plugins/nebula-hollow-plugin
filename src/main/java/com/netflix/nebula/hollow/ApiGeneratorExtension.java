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

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.List;

public class ApiGeneratorExtension {

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

    @Inject
    public ApiGeneratorExtension(ObjectFactory objects) {
        this.packagesToScan = objects.listProperty(String.class).empty();
        this.apiClassName = objects.property(String.class);
        this.apiPackageName = objects.property(String.class);
        this.getterPrefix = objects.property(String.class);
        this.classPostfix = objects.property(String.class);
        this.destinationPath = objects.property(String.class).convention("");
        this.parameterizeAllClassNames = objects.property(Boolean.class).convention(false);
        this.useAggressiveSubstitutions = objects.property(Boolean.class).convention(false);
        this.useErgonomicShortcuts = objects.property(Boolean.class).convention(true);
        this.usePackageGrouping = objects.property(Boolean.class).convention(true);
        this.useBooleanFieldErgonomics = objects.property(Boolean.class).convention(true);
        this.reservePrimaryKeyIndexForTypeWithPrimaryKey = objects.property(Boolean.class).convention(true);
        this.useHollowPrimitiveTypes = objects.property(Boolean.class).convention(true);
        this.restrictApiToFieldType = objects.property(Boolean.class).convention(true);
        this.useVerboseToString = objects.property(Boolean.class).convention(true);
        this.useGeneratedAnnotation = objects.property(Boolean.class).convention(false);
    }

    public ListProperty<String> getPackagesToScan() {
        return packagesToScan;
    }

    public void setPackagesToScan(List<String> value) {
        packagesToScan.set(value);
    }

    public Property<String> getApiClassName() {
        return apiClassName;
    }

    public void setApiClassName(String value) {
        apiClassName.set(value);
    }

    public Property<String> getApiPackageName() {
        return apiPackageName;
    }

    public void setApiPackageName(String value) {
        apiPackageName.set(value);
    }

    public Property<String> getGetterPrefix() {
        return getterPrefix;
    }

    public void setGetterPrefix(String value) {
        getterPrefix.set(value);
    }

    public Property<String> getClassPostfix() {
        return classPostfix;
    }

    public void setClassPostfix(String value) {
        classPostfix.set(value);
    }

    public Property<String> getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String value) {
        destinationPath.set(value);
    }

    public Property<Boolean> getParameterizeAllClassNames() {
        return parameterizeAllClassNames;
    }

    public void setParameterizeAllClassNames(boolean value) {
        parameterizeAllClassNames.set(value);
    }

    public Property<Boolean> getUseAggressiveSubstitutions() {
        return useAggressiveSubstitutions;
    }

    public void setUseAggressiveSubstitutions(boolean value) {
        useAggressiveSubstitutions.set(value);
    }

    public Property<Boolean> getUseErgonomicShortcuts() {
        return useErgonomicShortcuts;
    }

    public void setUseErgonomicShortcuts(boolean value) {
        useErgonomicShortcuts.set(value);
    }

    public Property<Boolean> getUsePackageGrouping() {
        return usePackageGrouping;
    }

    public void setUsePackageGrouping(boolean value) {
        usePackageGrouping.set(value);
    }

    public Property<Boolean> getUseBooleanFieldErgonomics() {
        return useBooleanFieldErgonomics;
    }

    public void setUseBooleanFieldErgonomics(boolean value) {
        useBooleanFieldErgonomics.set(value);
    }

    public Property<Boolean> getReservePrimaryKeyIndexForTypeWithPrimaryKey() {
        return reservePrimaryKeyIndexForTypeWithPrimaryKey;
    }

    public void setReservePrimaryKeyIndexForTypeWithPrimaryKey(boolean value) {
        reservePrimaryKeyIndexForTypeWithPrimaryKey.set(value);
    }

    public Property<Boolean> getUseHollowPrimitiveTypes() {
        return useHollowPrimitiveTypes;
    }

    public void setUseHollowPrimitiveTypes(boolean value) {
        useHollowPrimitiveTypes.set(value);
    }

    public Property<Boolean> getRestrictApiToFieldType() {
        return restrictApiToFieldType;
    }

    public void setRestrictApiToFieldType(boolean value) {
        restrictApiToFieldType.set(value);
    }

    public Property<Boolean> getUseVerboseToString() {
        return useVerboseToString;
    }

    public void setUseVerboseToString(boolean value) {
        useVerboseToString.set(value);
    }

    public Property<Boolean> getUseGeneratedAnnotation() {
        return useGeneratedAnnotation;
    }

    public void setUseGeneratedAnnotation(boolean value) {
        useGeneratedAnnotation.set(value);
    }
}
