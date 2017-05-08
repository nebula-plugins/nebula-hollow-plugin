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
import spock.lang.Ignore

class ApiGeneratorSpecification extends IntegrationSpec {

    def 'plugin applies'() {
        given:
        buildFile << """
        apply plugin: 'nebula.hollow'
        """

        when:
        runTasksSuccessfully('help')

        then:
        noExceptionThrown()
    }

    def 'generator task configures'() {
        given:
        buildFile << """
        apply plugin: 'nebula.hollow'

        hollow {
            packagesToScan = ['org.package1']
            apiClassName = 'MyApiClassName'
            apiPackageName = 'org.package3.api'
        }
        """

        when:
        runTasksSuccessfully('help')

        then:
        noExceptionThrown()
    }

    @Ignore
    def 'execution of generator task is successful'() {
        given:
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'nebula.hollow'
            
            sourceCompatibility = 1.7
            
            hollow {
                packagesToScan = ['org.package1', 'org.package2']
                apiClassName = 'MyApiClassName'
                apiPackageName = 'org.package3.api'
            }
                        
            dependencies {
                compile "com.netflix.hollow:hollow:2.+"
            }
        """.stripIndent()

        def packageInfoFile = createFile('src/main/java/org/package1/package-info.java')
        packageInfoFile << """
            package org.package1;
        """.stripIndent()

        def file = createFile('src/main/java/org/package1/Entity1.java')
        file << """
            package org.package1;
            
            public class Entity1 {
                
                private String stringField;
                private Integer integerField;
            }
        """.stripIndent()

        def fileInSubPackage = createFile('src/main/java/org/package1/subpackage/Entity2.java')
        fileInSubPackage << """
            package org.package1.subpackage;
            
            import java.util.List;
            import java.util.concurrent.atomic.AtomicBoolean;
            
            public class Entity2 {
                private List<AtomicBoolean> collectionField;
            }
        """.stripIndent()

        when:
        def result = runTasks('generateHollowConsumerApi')

        then:
        [
            'MyApiClassName.java',
            'Entity1HollowFactory.java',
            'Entity1TypeAPI.java',
            'IntegerTypeAPI.java',
            'StringTypeAPI.java',
            'Entity2HollowFactory.java',
            'Entity2TypeAPI.java',
            'ListOfAtomicBooleanTypeAPI.java'
        ].forEach { fileName ->
            assert getFile(fileName).exists()
        }
    }

    def getFile(String fileName) {
        new File(projectDir, '/src/main/java/org/package3/api/'.concat(fileName))
    }
}
