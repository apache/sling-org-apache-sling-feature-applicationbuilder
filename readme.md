[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=sling-org-apache-sling-feature-applicationbuilder-1.8)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-feature-applicationbuilder-1.8) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/feature.md)

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

For further documentation see: https://github.com/apache/sling-org-apache-sling-feature/blob/master/readme.md
