package io.goharbor.util.harborcli.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.goharbor.client.openapi.ApiClient;
import io.goharbor.client.openapi.ApiException;
import io.goharbor.client.openapi.apis.ProjectApi;
import io.goharbor.client.openapi.models.Project;
import io.goharbor.util.harborcli.config.BasicAuth;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Service
public class AuthHelper {
    private ApiClient getAPiClient() throws IOException {
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
        authcommand.subcommands().stream().filter(c->c.commandSpec().name().equalsIgnoreCase("login")).limit(1).forEach(AuthHelper::processAuthLogin);
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
            throw new CommandLine.ParameterException(command,exception.getMessage());
        }
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
}
