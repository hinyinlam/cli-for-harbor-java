/*
 * CLI for Harbor
 * Copyright 2021 VMware, Inc.
 *
 * This product is licensed to you under the Apache 2.0 license (the "License").  You may not use this product except in compliance with the Apache 2.0 License.
 *
 * This product may include a number of subcomponents with separate copyright notices and license terms. Your use of these subcomponents is subject to the terms and conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package io.goharbor.util.harborcli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.DefaultParameterNameDiscoverer;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.UsageMessageSpec;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class HarborCliApplication {

    @Autowired
    public CommandDispatcherService cds;

    @Autowired
    public OpenAPIParserService openAPIParserService;

    @Bean
    public CommandLineRunner buildHarborCmd(){
        return args -> {

            Map<String, List<Method>> apiMethodsMap = openAPIParserService.getApiMethods();

            List<CommandSpec> mainCommands = new ArrayList<>();

            for(String apiName: apiMethodsMap.keySet()){
                String topLevelCmdName = apiName.toLowerCase().replace("api", "" );
                CommandSpec topLevelCmdSpec = CommandSpec.create()
                        .name(topLevelCmdName)
                        .mixinStandardHelpOptions(true);

                List<Method> methodsInThisApi = apiMethodsMap.get(apiName);
                Map<String, CommandSpec> subcmdSpecMap = addSubcmdSpec(topLevelCmdName, methodsInThisApi);
                subcmdSpecMap.forEach((subCmdName, subCmdSpec) -> topLevelCmdSpec.addSubcommand(subCmdName,subCmdSpec));

                mainCommands.add(topLevelCmdSpec);
            }

            //Remember the login cmd that we manually created
            mainCommands = addAuthSubCommand(mainCommands);

            //Adding model data dump for easy consume
            mainCommands = addDumpModelExample(mainCommands);

            CommandSpec spec = CommandSpec.create()
                    .name("harbor")
                    .usageMessage(new UsageMessageSpec().description("CLI for Harbor - https://github.com/goharbor/harbor"))
                    .mixinStandardHelpOptions(true);

            mainCommands.forEach(cmd -> {
                spec.addSubcommand(cmd.name(),cmd);
            });

            CommandLine cmd = new CommandLine(spec);
            cmd.setParameterExceptionHandler(HarborCliApplication::invalidUserInput);
            cmd.setExecutionExceptionHandler(HarborCliApplication::runtimeException);
            cmd.setExecutionStrategy(cds);

            int exitCode = cmd.execute(args);
            if(exitCode==-2){
                spec.commandLine().usage(System.err);
            }
            System.exit(exitCode);
        };
    }

    private OptionSpec getSubcmdOptionSpec(Class<?> paramType, String paramName){
        OptionSpec.Builder specBuilder = OptionSpec.builder("--"+ paramName);
        if(paramName.equals("xRequestId") || paramName.equals("page") || paramName.equals("pageSize")) {
            return specBuilder.required(false).description(paramName).arity("0..1").defaultValue(null).type(paramType).build();
        }
        if(!paramType.getPackageName().equals("java.lang")){ //non-java lang types:
            return specBuilder.required(false).description("Complex kind - Use .json/.yaml file here").arity("0..1").type(String.class).build();
        }
        return specBuilder.required(false).description(paramName).arity("0..1").type(paramType).build();
    }

    private Map<String, CommandSpec> addSubcmdSpec(String parentCmdName, List<Method> ms) {
        Map<String, CommandSpec> allSubCmds = new HashMap<>();
        for(Method m: ms){
            String subCmdName = openAPIParserService.getCmdExternalName(parentCmdName, m);
            CommandSpec methodSubcmd = CommandSpec.create()
                    .name(subCmdName)
                    .mixinStandardHelpOptions(true);

            Parameter[] params = m.getParameters();
            String[] paramNames = new DefaultParameterNameDiscoverer().getParameterNames(m);
            for(int i=0; i< params.length; i++){
                Class<?> paramType = params[i].getType();
                String paramName = paramNames[i];
                if(paramName.equals("version")||paramName.equals("help")){
                    methodSubcmd = methodSubcmd.mixinStandardHelpOptions(false); //avoid --version or --help conflict
                }
                OptionSpec optionSpec = getSubcmdOptionSpec(paramType,paramName);
                methodSubcmd = methodSubcmd.addOption(optionSpec);
            }

            allSubCmds.put(subCmdName,methodSubcmd);
        }
        return allSubCmds;
    }



    public static void main(String[] args){
        SpringApplication.run(HarborCliApplication.class, args);
    }

    private static List<CommandSpec> addAuthSubCommand(List<CommandSpec> mainCommands) {
        CommandSpec loginSpec = CommandSpec.create()
                .name("login")
                .addOption(OptionSpec.builder("-u", "--username").paramLabel("USERNAME").required(true).arity("1").description("Username").type(String.class).build())
                .addOption(OptionSpec.builder("-p", "--password").paramLabel("PASSWORD").required(true).arity("0..1").interactive(true).type(String.class).build())
                .addOption(OptionSpec.builder("-a", "--api").paramLabel("API_ENDPOINT").required(true).arity("1").description("API endpoint to Harbor").type(String.class).build())
                .usageMessage(new UsageMessageSpec().description("Authentication with Harbor"));
        mainCommands.add(loginSpec);

        CommandSpec logoutSpec = CommandSpec.create()
                .name("logout")
                .usageMessage(new UsageMessageSpec().description("Logout by deleting locally saved auth config"));
        mainCommands.add(logoutSpec);

        return mainCommands;
    }

    private static List<CommandSpec> addDumpModelExample(List<CommandSpec> mainCommands){
        CommandSpec modelExample = CommandSpec.create()
                .name("modelexample")
                .addOption(OptionSpec.builder("-m").paramLabel("MODELNAME").required(true).arity("1").description("Model name in *Camel Case* eg: User / ChartVersion").type(String.class).build())
                .usageMessage(new UsageMessageSpec().description("Dump data Model for any complex type"));
        modelExample.mixinStandardHelpOptions(true);
        mainCommands.add(modelExample);
        return mainCommands;
    }


//    static void processProject(CommandLine.ParseResult projectCommand){
//        projectCommand.subcommands().stream().filter(c->c.commandSpec().name().equalsIgnoreCase("list")).limit(1).forEach(HarborCliApplication::processListProject);
//    }
//
//    static void processListProject(CommandLine.ParseResult listProjectCommand){
//        try {
//            ApiClient apiClient = getAPiClient();
//            ProjectApi projectApi = new ProjectApi(apiClient);
//            List<Project> projects = projectApi.listProjects(null, null, null, null, null, null);
//            projects.stream().forEach(p -> System.out.println(p.getName()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ApiException exception) {
//            exception.printStackTrace();
//        }
//    }


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
}
