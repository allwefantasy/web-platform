package tech.mlsql.serviceframework.platform.plugin;

import org.apache.commons.beanutils.MethodUtils;

import java.lang.reflect.Method;

/**
 * 25/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
public class ReflectUtils {
    public static Class[] paramsToTypes(Object... params) {
        Class[] clzz = new Class[params.length];
        int i = 0;
        for (Object tt : params) {
            clzz[i++] = tt.getClass();
        }
        return clzz;
    }

    public static Object method(Object obj, String methodName, Object... params) {
        try {

            Method method = null;
            try {
                method = obj.getClass().getDeclaredMethod(methodName, paramsToTypes(params));
            } catch (Exception e) {
                try {
                    method = obj.getClass().getMethod(methodName, paramsToTypes(params));
                } catch (Exception e1) {
                    method = MethodUtils.getMatchingAccessibleMethod(obj.getClass(), methodName, paramsToTypes(params));
                }
            }
            if (method == null) return null;
            method.setAccessible(true);
            return method.invoke(obj, params);
        } catch (Exception e) {
            return null;
        }
    }
}
