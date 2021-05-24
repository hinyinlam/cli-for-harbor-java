/*
 * CLI for Harbor
 * Copyright 2021 VMware, Inc.
 *
 * This product is licensed to you under the Apache 2.0 license (the "License").  You may not use this product except in compliance with the Apache 2.0 License.
 *
 * This product may include a number of subcomponents with separate copyright notices and license terms. Your use of these subcomponents is subject to the terms and conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package io.goharbor.util.harborcli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.goharbor.client.openapi.ApiClient;
import io.goharbor.util.harborcli.auth.AuthHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommandDispatcherService implements CommandLine.IExecutionStrategy {

    Logger logger = LoggerFactory.getLogger(CommandDispatcherService.class);

    @Autowired
    OpenAPIParserService apiParser;

    @Autowired
    AuthHelper authHelper;

    @Autowired
    ModelHelper modelHelper;

    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException, CommandLine.ParameterException {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) { return helpExitCode; }

        List<CommandLine.ParseResult> subcommands = parseResult.subcommands();
        if(subcommands.size()==0){ //invoke `harbor` cmd without subcommand -> show help
            return -2;
        }

        CommandLine.ParseResult api = parseResult.subcommands().get(0);
        String apiName = api.commandSpec().name();
        if(apiName.equals("login")){
            return authHelper.login(api);
        }
        if(apiName.equals("logout")){
            authHelper.logout();
            return 0; //success exit normally
        }
        if(apiName.equals("modelexample")){
            return modelHelper.dump(api);
        }

        //Let's call the real API:
        return executeAPICommand(api);

    }

    private int executeAPICommand(CommandLine.ParseResult api) {
        String apiName = api.commandSpec().name();
        String apiClassName = getApiClassNameFromActionName(apiName);


        CommandLine.ParseResult actionParseResult = api.subcommands().get(0);
        String actionName = actionParseResult.commandSpec().name();

        Object apiInstance = getApiClassInstance(apiClassName, true); //effectively: apiInstance = new ProjectApi(apiClient);
        if(apiInstance==null){
            return -1;
        }

        Method targetMethod = getTargetMethod(apiName, apiClassName, actionName); //effectively: method = projectApi.create
        if(targetMethod == null){
            logger.error("Method in API not found: " + apiName + "."+ actionName);
            return -1;
        }

        try {
            logger.debug("Invoking: " + apiInstance.getClass().getSimpleName() + " with (" + " TODO here )");

            Map<String, Object> optionSpecs = api.subcommands().get(0).matchedOptionsSet()
                    .stream().collect(
                            Collectors.toMap(os -> os.longestName().replace("--",""), CommandLine.Model.OptionSpec::getValue)
                    );
            List<String> unmatched = api.subcommands().get(0).unmatched();
            Parameter[] parameters = targetMethod.getParameters();
            String[] paramNames = new DefaultParameterNameDiscoverer().getParameterNames(targetMethod);
            Object[] executeParam = new Object[paramNames.length];
            for(int i=0; i< paramNames.length; i++){//maintain positional alignment
                    if(!parameters[i].getType().getPackageName().equals("java.lang")){
                        executeParam[i] = getComplexType(parameters[i].getType(), optionSpecs.get(paramNames[i])); //convert the file to complex type
                    }else {
                        executeParam[i] = optionSpecs.get(paramNames[i]);
                    }
            }

            Object result = targetMethod.invoke(apiInstance, executeParam); //TODO: Adding dynamically generated parameter list

            if(result!=null){
                ObjectMapper mapper = new ObjectMapper();
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
                //System.out.println(result); //TODO: cast the result back to the correct type (or can it?)
                //System.out.println(targetMethod.getReturnType());
            }
            return 0;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private Object getComplexType(Class<?> type, Object o) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            //String projectStr =mapper.writeValueAsString(new ProjectReq());
            //ProjectReq original = (ProjectReq) mapper.readValue(projectStr,type);
            //System.out.println(projectStr);
            Object complexType = mapper.readValue(new File((String)o), type);
            return complexType;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private Method getTargetMethod(String apiName, String apiKey, String actionName) {
        List<Method> methods = apiParser.getMethodsByApiName(apiKey);
        Method targetMethod = null;
        for(Method method : methods){
            String extName = apiParser.getCmdExternalName(apiName,method);
            if(extName.equals(actionName)){
                targetMethod = method;
            }
        }
        return targetMethod;
    }

    private String getApiClassNameFromActionName(String apiName) {
        String capitalApiName = apiName.substring(0, 1).toUpperCase() + apiName.substring(1); //project -> "Project"
        String apiKey = capitalApiName + "Api"; // "Project" -> "ProjectApi"
        return apiKey;
    }

    public Object getApiClassInstanceWithAPIClient(String apiKey, boolean printException, ApiClient apiClient){
        try {
            Class classRef = Class.forName("io.goharbor.client.openapi.apis."+ apiKey);
            Constructor constructor = classRef.getConstructor(ApiClient.class);
            Object apiInstance = constructor.newInstance(apiClient);
            return apiInstance;
        } catch (ClassNotFoundException e) {
            if(printException){e.printStackTrace();}
        } catch (InstantiationException e) {
            if(printException){e.printStackTrace();}
        } catch (InvocationTargetException e) {
            if(printException){e.printStackTrace();}
        } catch (NoSuchMethodException e) {
            if(printException){e.printStackTrace();}
        } catch (IllegalAccessException e) {
            if(printException){e.printStackTrace();}
        }
        return null;
    }
    public Object getApiClassInstance(String apiKey, boolean printException) {
        ApiClient apiClient = authHelper.getApiClient();
        if(apiClient==null){
            System.out.println("Please login using `harbor login` command");
            return null;
        }
        return getApiClassInstanceWithAPIClient(apiKey,printException,apiClient);
    }
}
