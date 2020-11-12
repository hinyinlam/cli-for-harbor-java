package io.goharbor.util.harborcli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.goharbor.client.openapi.ApiClient;
import io.goharbor.client.openapi.ApiException;
import io.goharbor.client.openapi.apis.ProjectApi;
import io.goharbor.client.openapi.models.Project;
import io.goharbor.util.harborcli.config.BasicAuth;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static picocli.CommandLine.Model.*;

@SpringBootApplication
public class HarborCliApplication {

    public static void main(String[] args) throws IOException {

        CommandSpec loginSpec = CommandSpec.create()
                .name("login")
                .addOption(OptionSpec.builder("-u", "--username").paramLabel("USERNAME").required(true).arity("1").description("Username").type(String.class).build())
                .addOption(OptionSpec.builder("-p", "--password").paramLabel("PASSWORD").required(true).arity("0..1").interactive(true).type(String.class).build())
                .addOption(OptionSpec.builder("-a", "--api").paramLabel("API_ENDPOINT").required(true).arity("1").description("API endpoint to Harbor").type(String.class).build())
                .usageMessage(new UsageMessageSpec().description("Authentication with Harbor"));

        CommandSpec authSpec = CommandSpec.create()
                .name("auth")
                .addSubcommand("login", loginSpec);

        CommandSpec listProjectSpec = CommandSpec.create()
                .name("list");

        CommandSpec projectSpec = CommandSpec.create()
                .name("project")
                .addSubcommand("list",listProjectSpec);

        CommandSpec spec = CommandSpec.create()
                .name("harbor")
                .usageMessage(new UsageMessageSpec().description("CLI for Harbor - https://github.com/goharbor/harbor"))
                .mixinStandardHelpOptions(true)
                .addSubcommand("auth", authSpec)
                .addSubcommand("project",projectSpec);


        CommandLine cmd = new CommandLine(spec);
        cmd.setParameterExceptionHandler(HarborCliApplication::invalidUserInput);
        cmd.setExecutionExceptionHandler(HarborCliApplication::runtimeException);
        cmd.setExecutionStrategy(HarborCliApplication::cmdRun);

        int exitCode = cmd.execute(args);
        System.exit(exitCode);

        //Map<Class<?>, List<Method>> apiMethods = retreiveAllAPIMethods("io.goharbor.client.openapi.apis");
        //SpringApplication.run(HarborCliApplication.class, args);
    }

    static int cmdRun(CommandLine.ParseResult pr){

        Integer helpExitCode = CommandLine.executeHelpRequest(pr);
        if (helpExitCode != null) { return helpExitCode; }

        List<CommandLine.ParseResult> subcommands = pr.subcommands();
        //Process auth command group:
        subcommands.stream().filter(c -> c.commandSpec().name().equalsIgnoreCase("auth")).limit(1).forEach(HarborCliApplication::processAuth);
        subcommands.stream().filter(c -> c.commandSpec().name().equalsIgnoreCase("project")).limit(1).forEach(HarborCliApplication::processProject);

        return 0;
    }

    static void processProject(CommandLine.ParseResult projectCommand){
        projectCommand.subcommands().stream().filter(c->c.commandSpec().name().equalsIgnoreCase("list")).limit(1).forEach(HarborCliApplication::processListProject);
    }

    static void processListProject(CommandLine.ParseResult listProjectCommand){
        try {
            ApiClient apiClient = getAPiClient();
            ProjectApi projectApi = new ProjectApi(apiClient);
            List<Project> projects = projectApi.listProjects(null, null, null, null, null, null);
            projects.stream().forEach(p -> System.out.println(p.getName()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApiException exception) {
            exception.printStackTrace();
        }
    }

    private static ApiClient getAPiClient() throws IOException {
        ApiClient client = new ApiClient();
        ObjectMapper mapper = new ObjectMapper();
        String userHome = System.getProperty("user.home");
        String filename = userHome + "/.harbor/basicAuth.json";
        BasicAuth basicAuth = mapper.readValue(new File(filename),BasicAuth.class);
        client.setUsername(basicAuth.username);
        client.setPassword(basicAuth.password);
        client.setBasePath(basicAuth.api);
        return client;
    }

    static void processAuth(CommandLine.ParseResult authcommand){
        authcommand.subcommands().stream().filter(c->c.commandSpec().name().equalsIgnoreCase("login")).limit(1).forEach(HarborCliApplication::processAuthLogin);
    }

    static void processAuthLogin(CommandLine.ParseResult loginCommand){
        //Set<OptionSpec> options = loginCommand.matchedOptionsSet();
        String username = loginCommand.matchedOption("username").getValue();
        String password = loginCommand.matchedOption("password").getValue();
        String api = loginCommand.matchedOption("api").getValue();
        System.out.println("Logging in using Username:" + username + "\nPassword: <hidden> " + "\napi: "+ api);
        tryLogin(username,password,api, loginCommand.commandSpec().commandLine());
    }

    static void tryLogin(String username, String password, String api, CommandLine command){
        ApiClient client = new ApiClient();
        client.setUsername(username);
        client.setPassword(password);
        client.setBasePath(api);
        ProjectApi projectApi = new ProjectApi(client);
        try {
            List<Project> projects = projectApi.listProjects(null,1l,1l,null,null,null);
            //At this point, no exception has thrown, thus login is success
            System.out.println("Login success");
            saveBasicAuthentication(username, password,api);
        } catch (ApiException exception) {
            //exception.printStackTrace();
            throw new CommandLine.ParameterException(command,exception.getMessage());
        }
//            ProjectReq projectReq = new ProjectReq();
//            projectReq.setProjectName("hinlam-java-client-project");
//            projectReq.setPublic(false);
    }

    private static void saveBasicAuthentication(String username, String password, String api) {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.setUsername(username);
        basicAuth.setPassword(password);
        basicAuth.setApi(api);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String value = mapper.writeValueAsString(basicAuth);
            String userHome = System.getProperty("user.home");
            String filename = userHome + "/.harbor/basicAuth.json";
            new File(userHome + "/.harbor/").mkdirs();
            new File(filename).createNewFile();
            try(FileWriter writer = new FileWriter(new File(filename))) {
                writer.write(value);
            }
            System.out.println("Credential Saved to " + filename);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // custom handler for runtime errors that does not print a stack trace
    static int runtimeException(Exception e,
                                CommandLine commandLine,
                                CommandLine.ParseResult parseResult) {
        commandLine.getErr().println("INTERNAL ERROR: " + e.getMessage());
        return CommandLine.ExitCode.SOFTWARE;
    }

    // custom handler for invalid input that does not print usage help
    static int invalidUserInput(CommandLine.ParameterException e, String[] strings) {
        CommandLine commandLine = e.getCommandLine();
        commandLine.getErr().println("ERROR: " + e.getMessage());
        commandLine.getErr().println("Try '"
                + commandLine.getCommandSpec().qualifiedName()
                + " --help' for more information.");
        return CommandLine.ExitCode.USAGE;
    }



    //@Bean
    /*
    public CommandLineRunner tryLogin(){
        return args -> {

//            ProjectApi api = new ProjectApi(client);
//            ProjectReq projectReq = new ProjectReq();
//            projectReq.setProjectName("hinlam-java-client-project");
//            projectReq.setPublic(false);
//            try {
//                api.createProject(projectReq, "1234");
//            }catch (ApiException exception){
//                ObjectMapper mapper = new ObjectMapper();
//                Errors errors = mapper.readValue(exception.getResponseBody(),Errors.class);
//                errors.getErrors().stream().forEach(System.out::println);
//            }

//            ArtifactApi artifactApi = new ArtifactApi(client);
//            try {
//                artifactApi.copyArtifact("hinlam-java-client-project", "hub.docker.com", "hub.docker.com/busybox:latest", "1235");
//            }catch (ApiException exception){
//                ObjectMapper mapper = new ObjectMapper();
//                Errors errors = mapper.readValue(exception.getResponseBody(),Errors.class);
//                errors.getErrors().stream().forEach(System.out::println);
//            }
        };
    }
    */
}
