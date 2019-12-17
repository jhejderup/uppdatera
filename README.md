# Uppdatera

Static Regression Detection for Automated Dependency Updates in Maven Projects


## Instructions

### Prerequisites
- JDK 8
- Maven
- git 

### Building

``` sh
git clone https://github.com/jhejderup/uppdatera.git
cd uppdatera
mvn package
```

### Running

``` sh
java -jar target/uppdatera-0.0.1-SNAPSHOT-jar-with-dependencies.jar [0] [1] [2] [3] [4] [5]
- [0] classpath_project => target/classes
- [1] classpath_depz => dep1.jar:dep2.jar:dep3.jar
- [2] groupId => org.slf4j
- [3] artifactId => slf4j-api
- [4] version_old => 1.7.25
- [5] version_new => 2.0.0
```

## Bucket list

- [x] :construction: Refactor components
- [ ] :chart_with_upwards_trend: Create neat reports
- [ ] :bullettrain_side: Dockerize setup for running on servers
- [ ] :inbox_tray: Improve logging
- [ ] :construction_worker: Extensive testing
- [ ] :checkered_flag: Launch it on Github Services
