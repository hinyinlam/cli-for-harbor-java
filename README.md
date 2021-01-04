# Command line for Harbor
## How to build?
This CLI depends on `harbor-client-java` - following instruction to `mvn install` in [Harbor Java Client](https://github.com/hinyinlam-pivotal/harbor-client-java)

Then `./mvnw clean package`

### Or you can download the cli jar file directly in github:
[Github release page](https://github.com/hinyinlam-pivotal/harbor-cli/releases)

## How to use

### Alias:

`alias harbor="java -jar harbor-cli-0.0.1-SNAPSHOT.jar"`

then you can uses `harbor` directly.

In future, will compile directly to OS dependent native image so you *will* not need aliasing.

### Login:
`harbor login --username <username> --password <password> --api <https_api>`

Example:

`harbor login --username myuser --password ASafePassword --api demo.goharbor.io`

### Project list:
`harbor project list`

Note: remember to `harbor login` first.

# ToDo:
Yes, I'm aware the code isn't good structured, nearly no functions.
But this is an MVP which allows anyone to use command line to interact with Harbor!

Next: Package as a native image in various OS and add back tons of sub-command

### Reference Documentation
* [Official Harbor](https://goharbor.io/)
* [Harbor Java Client](https://github.com/hinyinlam-pivotal/harbor-client-java)
* [Picocli](https://picocli.info/)
