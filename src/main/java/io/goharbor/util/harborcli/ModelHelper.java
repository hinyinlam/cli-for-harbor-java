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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Service
public class ModelHelper {

    @Autowired
    OpenAPIParserService openAPIParserService;

    public int dump(CommandLine.ParseResult dumpCommand) {
        String modelName = dumpCommand.matchedOption("m").getValue();
        Class<?> model = openAPIParserService.getModelByName(modelName);
        if(model==null){
            System.out.println("Model: " + modelName + " Not found!");
            System.exit(-1);
        }
        ObjectMapper mapper = new ObjectMapper();
        Constructor constructor = null;
        try {
            constructor = model.getConstructor();
            Object modelInstance = constructor.newInstance();
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(modelInstance));
            return 0;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
