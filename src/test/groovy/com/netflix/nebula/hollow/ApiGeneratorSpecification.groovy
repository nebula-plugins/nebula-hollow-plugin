/**
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
package com.netflix.nebula.hollow

import nebula.test.IntegrationSpec

class ApiGeneratorSpecification extends IntegrationSpec {

    // TODO: remove mavenLocal() repositories when plugin will be published
    def setup() {
        buildFile << """
            buildscript {
                repositories {
                    mavenLocal()
                    jcenter()
                }
                dependencies {
                    classpath "com.netflix.nebula:nebula-hollow-plugin:0.1"
                }
            }

            group = 'com.netflix.nebula.hollow'
            
            apply plugin: 'java'
            apply plugin: 'nebula.hollow-plugin'
            
            sourceCompatibility = 1.7
            
            hollow {
                packagesToScan = ['org.package1', 'org.package2']
                apiClassName = 'MyApiClassName'
                apiPackageName = 'org.package3.api'
            }
                        
            repositories {
                mavenLocal()
                jcenter()
            }
            
            dependencies {
                compile "com.netflix.hollow:hollow:2.3.1"
            }
        """.stripIndent()

        def file = createFile('src/main/java/org/package1/TestEntity.java')
        file << """
            package org.package1;
            
            import java.util.List;
            import java.util.concurrent.atomic.AtomicBoolean;
            
            public class TestEntity {
                
                private String stringField;
                private Integer integerField;
                private List<AtomicBoolean> collectionField;
            }
        """.stripIndent()
    }

    def "execution of generator task is successful"() {
        when:
        runTasks('generateHollowConsumerApi')

        then:
        expectedFiles.forEach { fileName ->
            assert getFile(fileName).exists() == true
        }
    }

    def getFile(String fileName) {
        new File(projectDir, '/src/main/java/org/package3/api/'.concat(fileName))
    }

    def expectedFiles = ['MyApiClassName.java', 'TestEntityHollowFactory.java', 'TestEntityTypeAPI.java',
                         'StringTypeAPI.java', 'ListOfAtomicBooleanTypeAPI.java', 'IntegerTypeAPI.java']
}
