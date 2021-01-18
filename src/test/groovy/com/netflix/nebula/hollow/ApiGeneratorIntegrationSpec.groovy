/**
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
package com.netflix.nebula.hollow

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult
import spock.lang.Subject

@Subject(ApiGeneratorTask)
class ApiGeneratorIntegrationSpec extends IntegrationSpec {

    def 'plugin applies'() {
        given:
        buildFile << """
        apply plugin: 'java'
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
        apply plugin: 'java'        
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

    def 'execution of generator task is successful'() {
        given:
        String destinationSrcFolder = '/src/main/java/com/netflix/nebula/hollow/test/api'
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'nebula.hollow'
            
            sourceCompatibility = 1.8
            
            hollow {
                packagesToScan = ['com.netflix.nebula.hollow.test']
                apiClassName = 'MovieAPI'
                apiPackageName = 'com.netflix.nebula.hollow.test.api'
            }
                     
            repositories {
               jcenter()
            }
               
            dependencies {
                implementation "com.netflix.hollow:hollow:3.+"
            }
        """.stripIndent()

        def packageInfoFile = createFile('src/main/java/com/netflix/nebula/hollow/test/package-info.java')
        packageInfoFile << """
            package com.netflix.nebula.hollow.test;
        """.stripIndent()

        def moviefile = createFile('src/main/java/com/netflix/nebula/hollow/test/Movie.java')
        moviefile << """package com.netflix.nebula.hollow.test;

import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey;
import java.util.Set;

@HollowPrimaryKey(fields={"id"})
public class Movie {
    long id;
    String title;
    int releaseYear;
    Set<Actor> actors;
    
    public Movie(long id, String title, int releaseYear, Set<Actor> actors) {
        this.id = id;
        this.title = title;
        this.releaseYear = releaseYear;
        this.actors = actors;
    }
}
        """.stripIndent()

        def authorFile = createFile('src/main/java/com/netflix/nebula/hollow/test/Actor.java')
        authorFile << """package com.netflix.nebula.hollow.test;

public class Actor {
    String name;
    
    public Actor(String name) {
        this.name = name;
    }
}
 """.stripIndent()

        when:
        ExecutionResult result = runTasks('generateHollowConsumerApi')

        then:
        result.success

        and:
        [
            '/Movie.java',
            '/Actor.java',
            '/MovieAPI.java',
            '/accessor/ActorDataAccessor.java',
            '/accessor/MovieDataAccessor.java',
            '/collections/SetOfActor.java',
            '/core/ActorDelegate.java',
            '/core/ActorDelegateCachedImpl.java',
            '/core/ActorHollowFactory.java',
            '/core/ActorTypeAPI.java',
            '/core/MovieAPIFactory.java',
            '/core/MovieDelegate.java',
            '/core/MovieDelegateCachedImpl.java',
            '/core/MovieDelegateLookupImpl.java',
            '/core/MovieHollowFactory.java',
            '/core/MovieTypeAPI.java',
            '/core/SetOfActorHollowFactory.java',
            '/core/SetOfActorTypeAPI.java',
            '/core/ActorDelegateLookupImpl.java',
            '/core/MovieAPIFactory.java',
            '/index/MovieAPIHashIndex.java',
            '/index/MoviePrimaryKeyIndex.java',
            '/index/MovieUniqueKeyIndex.java'
        ].forEach { fileName ->
            assert getFile(destinationSrcFolder, fileName).exists()
        }
    }

    def 'execution of generator - add class postfix'() {
        given:
        String destinationSrcFolder = '/src/main/java/com/netflix/nebula/hollow/test/postfix/api'
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'nebula.hollow'
            
            sourceCompatibility = 1.8
            
            hollow {
                packagesToScan = ['com.netflix.nebula.hollow.test.postfix']
                apiClassName = 'MovieAPI'
                apiPackageName = 'com.netflix.nebula.hollow.test.postfix.api'
                classPostfix = 'Hollow'
            }
                     
            repositories {
               jcenter()
            }
               
            dependencies {
                implementation "com.netflix.hollow:hollow:3.+"
            }
        """.stripIndent()

        def moviefile = createFile('src/main/java/com/netflix/nebula/hollow/test/postfix/Movie.java')
        moviefile << """package com.netflix.nebula.hollow.test.postfix;

import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey;

@HollowPrimaryKey(fields={"id"})
public class Movie {
    long id;
    
    public Movie(long id) {
        this.id = id;
    }
}
        """.stripIndent()

        when:
        ExecutionResult result = runTasks('generateHollowConsumerApi')

        then:
        result.success

        and:
        getFile(destinationSrcFolder, '/MovieHollow.java').exists()
    }

    def 'execution of generator - add getter prefix'() {
        given:
        String destinationSrcFolder = '/src/main/java/com/netflix/nebula/hollow/test/getterprefix/api'
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'nebula.hollow'
            
            sourceCompatibility = 1.8
            
            hollow {
                packagesToScan = ['com.netflix.nebula.hollow.test.getterprefix']
                apiClassName = 'MovieAPI'
                apiPackageName = 'com.netflix.nebula.hollow.test.getterprefix.api'
                getterPrefix = '_'
            }
                     
            repositories {
               jcenter()
            }
               
            dependencies {
                implementation "com.netflix.hollow:hollow:3.+"
            }
        """.stripIndent()

        def moviefile = createFile('src/main/java/com/netflix/nebula/hollow/test/getterprefix/Movie.java')
        moviefile << """package com.netflix.nebula.hollow.test.getterprefix;

import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey;

@HollowPrimaryKey(fields={"id"})
public class Movie {
    long id;
    
    public Movie(long id) {
        this.id = id;
      
    }
}
        """.stripIndent()

        when:
        ExecutionResult result = runTasks('generateHollowConsumerApi')

        then:
        result.success

        and:
        File file = getFile(destinationSrcFolder, '/Movie.java')
        file.exists()

        and:
        file.text.contains("_getId()")
    }

    def 'generateHollowConsumerApi task is not present if java plugin is not present'() {
        given:
        buildFile << """
            apply plugin: 'nebula.hollow'
                     
            repositories {
               jcenter()
            }
               
            dependencies {
                implementation "com.netflix.hollow:hollow:3.+"
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasks('generateHollowConsumerApi')

        then:
        !result.success
    }

    def 'generateHollowConsumerApi is not present if nebula plugin is loaded before java plugin'() {
        given:
        buildFile << """
            apply plugin: 'nebula.hollow'
            apply plugin: 'java'
    
            repositories {
               jcenter()
            }
               
            dependencies {
                implementation "com.netflix.hollow:hollow:3.+"
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasks('generateHollowConsumerApi')

        then:
        !result.success
        result.failure.message.contains("Task 'generateHollowConsumerApi' not found in root project")
    }

    def 'execution fails if hollow block is present and plugin is not present'() {
        given:
        buildFile << """
            apply plugin: 'nebula.hollow'
            
            hollow {
                packagesToScan = ['com.netflix.nebula.hollow.test']
                apiClassName = 'MovieAPI'
                apiPackageName = 'com.netflix.nebula.hollow.test.api'
            }
                                 
            repositories {
               jcenter()
            }
               
            dependencies {
                implementation "com.netflix.hollow:hollow:3.+"
            }
        """.stripIndent()

        when:
        ExecutionResult result = runTasks('generateHollowConsumerApi')

        then:
        !result.success
    }

    def 'execution of generator - fails when required config is missing'() {
        given:
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'nebula.hollow'
            
            sourceCompatibility = 1.8
            
            hollow {
                packagesToScan = ['com.netflix.nebula.hollow.test']
            }
                     
            repositories {
               jcenter()
            }
               
            dependencies {
                implementation "com.netflix.hollow:hollow:3.+"
            }
        """.stripIndent()


        when:
        ExecutionResult result = runTasks('generateHollowConsumerApi')

        then:
        !result.success
        result.standardError.contains('Specify buildscript as per plugin readme | apiClassName, apiPackageName and packagesToScan configuration values must be present')
    }


    def getFile(String folder, String fileName) {
        new File(projectDir, folder.concat(fileName))
    }
}
