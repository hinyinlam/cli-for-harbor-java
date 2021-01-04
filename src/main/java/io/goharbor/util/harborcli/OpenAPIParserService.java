package io.goharbor.util.harborcli;

import com.google.common.reflect.ClassPath;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

@Service
public class OpenAPIParserService {

    private static final LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    Logger logger = LoggerFactory.getLogger(OpenAPIParserService.class);

    public OpenAPIParserService() throws IOException {
        apiMethods = retreiveAllAPIMethods("io.goharbor.client.openapi.apis");
    }

    @Getter
    private Map<String, List<Method>> apiMethods;

    private Map<String, List<Method>> retreiveAllAPIMethods(String apisPackage) throws IOException {
        Map<String, List<Method>> apiMethod = new HashMap<>();

        ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
        Set<ClassPath.ClassInfo> classInfos = classPath.getTopLevelClasses(apisPackage);

        for(ClassPath.ClassInfo info: classInfos) {
            Class<?> api = info.load();
            Method[] declaredMethods = api.getDeclaredMethods();
            List<Method> methods = this.getWantedMethods(Arrays.asList(declaredMethods));
            apiMethod.put(api.getSimpleName(),methods);
        }
        return apiMethod;
    }

    public List<Method> getWantedMethods(List<Method> ms) {
        List<Method> wantedMethods = new ArrayList<>();
        for (int i = 0; i < ms.size(); i++) {
            Method m = ms.get(i);
            if (Modifier.isPublic(m.getModifiers()) &&
                    //m.getReturnType().getName().equalsIgnoreCase("void") &&
                    !m.getName().equals("setApiClient") &&
                    !m.getName().equals("getApiClient") &&
                    !m.getReturnType().getSimpleName().equals("Call") &&
                    !m.getReturnType().getSimpleName().equals("ApiResponse")
            ) {
                wantedMethods.add(m);
            }
        }
        return wantedMethods;
    }

    public String getCmdExternalName(String parentCmdName, Method m) {
        //Replace subcommand name from long to "listAuditLogs" -> "list", "getProjectDeletable" -> "getdeleteable"
        //So that the full command will be `harbor project getdeletable` means getProjectDeletable method in ProjectAPI
        String parentPlural = parentCmdName.replaceAll("y$", "ies" );
        String subCmdName = m.getName().toLowerCase()
                .replace(parentCmdName +"s", "")
                .replace(parentCmdName,"")
                .replace(parentPlural,"");
        return subCmdName;
    }

    public static Map<String, Class<?>> getParameterNameTypePairs(Method m){
        Map<String, Class<?>> nameTypeMap = new HashMap<>();
        Parameter[] ps = m.getParameters();
        String[] pNames = discoverer.getParameterNames(m);
        for (int j = 0; j < ps.length; j++) {
            nameTypeMap.put(pNames[j], ps[j].getType());
            System.out.println(ps[j].getType().getName() + " " + pNames[j]);
        }
        return nameTypeMap;
    }

    public List<Method> getMethodsByApiName(String apiKey) {
        return this.apiMethods.get(apiKey);
    }
}
