package io.goharbor.util.harborcli;

import io.goharbor.client.openapi.ApiClient;
import io.goharbor.client.openapi.apis.ProjectApi;
import io.goharbor.util.harborcli.auth.AuthHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Service
public class CommandDispatcherService implements CommandLine.IExecutionStrategy {

    @Autowired
    OpenAPIParserService apiParser;

    @Autowired
    AuthHelper authHelper;

    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException, CommandLine.ParameterException {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) { return helpExitCode; }

        //TODO: Move this to another place -- Implement login command
        ApiClient apiClient = new ApiClient();
//        apiClient.setUsername(System.getenv("HARBOR_USERNAME"));
//        apiClient.setPassword(System.getenv("HARBOR_PASSWORD"));
//        apiClient.setBasePath(System.getenv("HARBOR_BASE_URL"));
        //EOL

        CommandLine.ParseResult api = parseResult.subcommands().get(0);
        String apiName = api.commandSpec().name();
        if(apiName.equals("login")){
            authHelper.login(api);
            return 0;
        }

        String actionName = api.subcommands().get(0).commandSpec().name();

        Map<String, List<Method>> apiMethodsMap = apiParser.getApiMethods();
        String capitalApiName = apiName.substring(0, 1).toUpperCase() + apiName.substring(1); //project -> "Project"
        String apiKey = capitalApiName + "Api"; // "Project" -> "ProjectApi"

        List<Method> methods = apiMethodsMap.get(apiKey);
        for(Method method : methods){
            String extName = apiParser.getCmdExternalName(apiName,method);
            if(extName.equals(actionName)){
                System.out.println("Match API: " + apiKey + " method: " + method.getName());
                try {
                    Class classRef = Class.forName("io.goharbor.client.openapi.apis."+ apiKey);
                    Constructor constructor = classRef.getConstructor(ApiClient.class);
                    Object apiInstance = constructor.newInstance(apiClient);
                    Object result = method.invoke(apiInstance, null, null, null, null, null, null); //TODO: Adding dynamically generated parameter list
                    if(result!=null){
                        System.out.println(result); //TODO: cast the result back to the correct type (or can it?)
                        System.out.println(method.getReturnType());
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }

        return 0;
    }
}
