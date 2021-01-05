# Unofficial command line for Harbor
This is not yet an offical project for [Harbor open source Registry](https://goharbor.io/)

## How to use
Download the cli jar file directly in github:
[Github release page](https://github.com/hinyinlam-pivotal/harbor-cli/releases)

### Alias:

`alias harbor="java -jar harbor-cli-0.0.1-SNAPSHOT.jar"`

then you can uses `harbor` directly.

In future, will compile directly to OS dependent native image so you *will* not need aliasing.

### Login:
`harbor login --username <username> --password <password> --api <https_api>`

Example:

`harbor login --username myuser --password ASafePassword --api demo.goharbor.io`

<p align="center"><img src="/doc/login-demo.gif?raw=true"/></p>

### Project:
`harbor project --help`

<p align="center"><img src="/doc/project-demo.gif?raw=true"/></p>

### Repo:
`harbor repository --help`

<p align="center"><img src="/doc/repo-demo.gif?raw=true"/></p>

### Artifact and Scanning:
`harbor artifact --help`

<p align="center"><img src="/doc/repo-demo.gif?raw=true"/></p>

# ToDo:
1. Package as a native image in various OS and add back tons of sub-command

2. More options for output

3. Output examples for .JSON / .YAML for complex type

Please feel free to open issue for feature requests.

## How to build?
This CLI depends on `harbor-client-java` - following instruction to `mvn install` in [Harbor Java Client](https://github.com/hinyinlam-pivotal/harbor-client-java)

Then `./mvnw clean package`

### Reference Documentation
* [Official Harbor](https://goharbor.io/)
* [Harbor Java Client](https://github.com/hinyinlam-pivotal/harbor-client-java)
* [Picocli](https://picocli.info/)
