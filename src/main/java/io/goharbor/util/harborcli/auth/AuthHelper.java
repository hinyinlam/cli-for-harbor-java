package io.goharbor.util.harborcli.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.goharbor.client.openapi.ApiClient;
import io.goharbor.client.openapi.ApiException;
import io.goharbor.client.openapi.apis.ProjectApi;
import io.goharbor.client.openapi.models.Project;
import io.goharbor.util.harborcli.config.BasicAuth;
import lombok.Getter;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Service
public class AuthHelper {

    @Getter
    private ApiClient apiClient;

    public AuthHelper(){
        apiClient = loadBasicAuthentication(); //load existing API Client
    }

    private ApiClient loadBasicAuthentication(){
        ApiClient client = new ApiClient();
        ObjectMapper mapper = new ObjectMapper();
        File harborAuthConfigFile = null;
        try {
            harborAuthConfigFile = getHarborAuthConfig();
            if(harborAuthConfigFile.length()==0l){
                System.out.println("No existing authentication found, please use `harbor login` command first");
                return null;
            }
            BasicAuth basicAuth = mapper.readValue(harborAuthConfigFile,BasicAuth.class);
            client.setUsername(basicAuth.username);
            client.setPassword(basicAuth.password);
            client.setBasePath(basicAuth.api);
            return client;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private String correctApiURL(String api) throws MalformedURLException {
        //Assume the user input "harbor.abc.com" without full base URL - should be: https://harbor.abc.com/api/v2.0
        if(!api.contains("https://")){
            api = "https://" + api;
        }
        URL harborApiURL = new URL(api);
        if(!harborApiURL.getPath().equals("/api/v2.0")){
            api = api + "/api/v2.0";
        }
        URL changedURL = new URL(api); // should throw exception if our "fixes" on URL is invalid
        return api;
    }

    public boolean login(CommandLine.ParseResult loginCommand){
        //Set<OptionSpec> options = loginCommand.matchedOptionsSet();
        String username = loginCommand.matchedOption("username").getValue();
        String password = loginCommand.matchedOption("password").getValue();
        String api = loginCommand.matchedOption("api").getValue();
        //Check API URL correctness
        try {
            api = correctApiURL(api);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

        System.out.println("Try logging in using\nUsername:" + username + "\nPassword: <hidden> " + "\napi: "+ api);
        //Make an API request to valid if the credentials are correct
        try {
            tryLogin(username, password, api);
            return true;
        }catch (ApiException exception){
            exception.printStackTrace();
        }
        return false;
    }

    private void tryLogin(String username, String password, String api) throws ApiException{
        ApiClient tryLoginClient = new ApiClient();
        tryLoginClient.setUsername(username);
        tryLoginClient.setPassword(password);
        tryLoginClient.setBasePath(api);
        ProjectApi projectApi = new ProjectApi(tryLoginClient);
        List<Project> projects = projectApi.listProjects(null,1l,1l,null,null,null);
        //At this point, no exception has thrown, thus login is success
        System.out.println("Login success");
        saveBasicAuthentication(username, password,api);
        //refresh this API client(may not be useful as this app is going to exit) for any success verified and saved credential
        this.apiClient = loadBasicAuthentication();
    }

    private File harborConfigDir = new File(System.getProperty("user.home") + File.separator + ".harbor");

    private void saveBasicAuthentication(String username, String password, String api){
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.setUsername(username);
        basicAuth.setPassword(password);
        basicAuth.setApi(api);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String value = mapper.writeValueAsString(basicAuth);

            File basicAuthConfig = getHarborAuthConfig();

            try(FileWriter writer = new FileWriter(basicAuthConfig)){
                writer.write(value);
            }
            System.out.println("Credential Saved to " + basicAuthConfig);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getHarborAuthConfig() throws IOException {
        if(!harborConfigDir.exists()){
            harborConfigDir.mkdir();
        }

        File basicAuthConfig = new File(harborConfigDir,"basicAuth.json");
        if(!basicAuthConfig.exists()){
            basicAuthConfig.createNewFile();
            return basicAuthConfig;
        }
        return basicAuthConfig;
    }

}
