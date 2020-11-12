# Command line for Harbor
##How to build?
This CLI depends on `harbor-client-java` - following instruction to `mvn install` in [Harbor Java Client](https://github.com/hinyinlam-pivotal/harbor-client-java)

Then `./mvnw clean package`

## How to use
### Login:
`java -jar target/harbor-cli-0.0.1-SNAPSHOT.jar auth login --username <username> --password <password> --api <https_api>`

Example:

`java -jar target/harbor-cli-0.0.1-SNAPSHOT.jar auth login --username myuser --password ASafePassword --api https://demo.goharbor.io/api/v2.0`

### Project list:
`java -jar target/harbor-cli-0.0.1-SNAPSHOT.jar project list`

Note: remember to `auth login` first.

# ToDo:
Yes, I'm aware the code isn't good structured, nearly no functions.
But this is an MVP which allows anyone to use command line to interact with Harbor!

Next: Package as a native image in various OS and add back tons of sub-command

### Reference Documentation
* [Official Harbor](https://goharbor.io/)
* [Harbor Java Client](https://github.com/hinyinlam-pivotal/harbor-client-java)
* [Picocli](https://picocli.info/)
