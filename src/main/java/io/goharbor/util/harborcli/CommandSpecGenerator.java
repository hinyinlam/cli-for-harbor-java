package io.goharbor.util.harborcli;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.ClassPath;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

public class CommandSpecGenerator {

    private static final LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    public static Map<Class<?>, List<Method>> retreiveAllAPIMethods(String apisPackage) throws IOException {
        Map<Class<?>, List<Method>> apiMethod = new HashMap<>();

        ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
        Set<ClassPath.ClassInfo> classInfos = classPath.getTopLevelClasses(apisPackage);

        for(ClassPath.ClassInfo info: classInfos) {
            Class<?> api = info.load();
            Method[] declaredMethods = api.getDeclaredMethods();
            List<Method> methods = filterMethod(Arrays.asList(declaredMethods));
            apiMethod.put(api,methods);
//            for (int i = 0; i < methods.size(); i++) {
//                Method m = methods.get(i);
//                System.out.println(api.getSimpleName() + "." + m.getName());
//                Map<String, Class<?>> nameTypeMap = getParameterNameTypePairs(m);
//                nameTypeMap.forEach((name, type) -> System.out.println(name + " : " + type));
//            }
        }
        return apiMethod;
    }

    public static List<Method> filterMethod(List<Method> ms) {
        List<Method> filtered = new ArrayList<>();
        for (int i = 0; i < ms.size(); i++) {
            Method m = ms.get(i);
            if (Modifier.isPublic(m.getModifiers()) &&
                    m.getReturnType().getName() == "void" &&
                    m.getName() != "setApiClient") {
                filtered.add(m);
            }
        }
        return filtered;
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
}
