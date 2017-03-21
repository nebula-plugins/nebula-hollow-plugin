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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApiGeneratorPlugin implements Plugin<Project> {

    /**
     * Task depends on build, because we need .class files for {@link URLClassLoader} to load them to
     * {@link com.netflix.hollow.core.write.objectmapper.HollowObjectMapper}
     */
    @Override
    public void apply(Project project) {
        Map<String, Object> taskPropertiesMap = new HashMap<>();
        taskPropertiesMap.put("name", "generateHollowConsumerApi");
        taskPropertiesMap.put("group", "hollow");
        taskPropertiesMap.put("type", ApiGeneratorTask.class);
        taskPropertiesMap.put("dependsOn", Collections.singletonList("build"));
        project.getTasks().create(taskPropertiesMap);

        project.getExtensions().create("hollow", ApiGeneratorExtension.class);
    }
}
