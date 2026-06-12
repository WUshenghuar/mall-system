package com.mall.security.aspect;

public class DataScopeContext {
    private static final ThreadLocal<String> scopeSql = new ThreadLocal<>();

    public static void setScopeSql(String sql) { scopeSql.set(sql); }

    public static String getScopeSql() { return scopeSql.get(); }

    public static void clear() { scopeSql.remove(); }
}