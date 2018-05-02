# Feature Model Application Builder

Build an Application Definition from one or more Feature Files.

The Application Builder can be run from the command line by executing the following command:

```
java org.apache.sling.feature.applicationbuilder.impl.Main
```

For example:

```
java org.apache.sling.feature.applicationbuilder.impl.Main -d <directory with feature files> -u ~/.m2/repository -o generated-app.json
```