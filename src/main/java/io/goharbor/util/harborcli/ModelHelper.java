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
