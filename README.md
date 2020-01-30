=======
# Hollow API Generator Nebula Plugin
![Support Status](https://img.shields.io/badge/nebula-supported-brightgreen.svg)
[![Build Status](https://travis-ci.org/nebula-plugins/nebula-hollow-plugin.svg?branch=master)](https://travis-ci.org/nebula-plugins/gradle-contacts-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/nebula-hollow-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/nebula-plugins/gradle-contacts-plugin?branch=master)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-hollow-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)


This plugin provides task for generating hollow consumer api, that is 
described [here](http://hollow.how/getting-started/#consumer-api-generation)

## Usage
In order to use, add plugin dependency to your buildscript
```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "com.netflix.nebula:nebula-hollow-plugin:1.+"
    }
}
```
apply it

```
apply plugin: 'nebula.hollow'
```

and configure (you have to specify all the three values to work)
```
hollow {
    packagesToScan = ['org.example.data', 'org.example.other.data']
    apiClassName = 'MyApi'
    apiPackageName = 'org.example.consumer.api'
}
```

where:

- `packagesToScan` - packages with your data classes, **note that scan is recursive**
- `apiClassName` - class name for your [api implementation](https://github.com/Netflix/hollow/blob/master/hollow/src/main/java/com/netflix/hollow/api/custom/HollowAPI.java) 
- `apiPackageName` - target package in your project for api-related sources

optional values:

- `getterPrefix` - prefix for getter methods. Ex. `_getId()`
- `classPostfix` - postfix for classes. Ex. `MovieHollow.java`
- `usePackageGrouping` - will create Hollow artifacts separated by sub-packages: index, core, collections
- `parameterizeAllClassNames` - if true, all methods which return a Hollow Object will be parameterized.  This is useful when alternate implementations are desired for some types. defaults to `true`
- `useAggressiveSubstitutions` - override generated classnames for type names corresponding to any class in the java.lang package. For more details please refer to [`HollowCodeGenerationUtils`](https://github.com/Netflix/hollow/blob/master/hollow/src/main/java/com/netflix/hollow/api/codegen/HollowCodeGenerationUtils.java). defaults to `true`
- `useErgonomicShortcuts` - generates API shortcuts. For more details please refer to [`HollowErgonomicAPIShortcutsTest`](https://github.com/Netflix/hollow/blob/master/hollow/src/test/java/com/netflix/hollow/api/codegen/HollowErgonomicAPIShortcutsTest.java). defaults to `true`
- `reservePrimaryKeyIndexForTypeWithPrimaryKey` - specify to only generate PrimaryKeyIndex for Types that has PrimaryKey defined. defaults to `true`
- `useHollowPrimitiveTypes` - specify to use Hollow Primitive Types instead of generating them per project. defaults to `true`
- `restrictApiToFieldType` - api code only generates `get<FieldName>` with return type as per schema. defaults to `true`
- `useVerboseToString` - will implement `toString()` method for hollow objects with `HollowRecordStringifier().stringify(this)`. defaults to `true`
- `sourcesExtension` - the extension of your data classes. defaults to `".java"`
- `filesToExclude` - files to exclude when scanning for data classes. defaults to `["package-info.java", "module-info.java"]`
- `relativeCompileClassPaths` - the relative classpath of your compiled classes. defaults to `["/build/classes/main/", "/build/classes/java/main/"]`
- `relativeDestinationPath` - the relative path to the api-related sources will be generated. defaults to `"/src/main/java/"`
- `relativeSourcesPath` - the relative path to your data classes. defaults to `"/src/main/java/"`

For more information, please refer to [`AbstractHollowAPIGeneratorBuilder`](https://github.com/Netflix/hollow/blob/master/hollow/src/main/java/com/netflix/hollow/api/codegen/AbstractHollowAPIGeneratorBuilder.java)
launch task:

`gradle generateHollowConsumerApi`
