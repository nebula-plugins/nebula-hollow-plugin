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

import java.util.Arrays;
import java.util.List;

public class ApiGeneratorExtension {

    public List<String> packagesToScan;
    public String apiClassName;
    public String apiPackageName;
    public String getterPrefix;
    public String classPostfix;
    public String sourcesExtension = ".java";
    public List<String> filesToExclude = Arrays.asList("package-info.java", "module-info.java");
    public List<String> relativeCompileClassPaths = Arrays.asList("/build/classes/main/", "/build/classes/java/main/");
    public String relativeDestinationPath = "/src/main/java/";
    public String relativeSourcesPath = "/src/main/java/";
    public boolean parameterizeAllClassNames = false;
    public boolean useAggressiveSubstitutions = false;
    public boolean useErgonomicShortcuts = true;
    public boolean usePackageGrouping = true;
    public boolean useBooleanFieldErgonomics = true;
    public boolean reservePrimaryKeyIndexForTypeWithPrimaryKey = true;
    public boolean useHollowPrimitiveTypes = true;
    public boolean restrictApiToFieldType = true;
    public boolean useVerboseToString = true;
}
