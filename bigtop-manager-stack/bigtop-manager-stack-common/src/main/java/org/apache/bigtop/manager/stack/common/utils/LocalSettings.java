package org.apache.bigtop.manager.stack.common.utils;


import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.bigtop.manager.common.message.type.pojo.ClusterInfo;
import org.apache.bigtop.manager.common.message.type.pojo.ComponentInfo;
import org.apache.bigtop.manager.common.message.type.pojo.RepoInfo;
import org.apache.bigtop.manager.common.utils.JsonUtils;

import java.io.File;
import java.util.*;

import static org.apache.bigtop.manager.common.constants.Constants.STACK_CACHE_DIR;
import static org.apache.bigtop.manager.common.constants.HostCacheConstants.*;

@Slf4j
public class LocalSettings {

    public static Object configurations(String service, String type, String key, Object defaultValue) {
        Map<String, Object> configMap = configurations(service, type);
        return configMap.getOrDefault(key, defaultValue);
    }

    public static Map<String, Object> configurations(String service, String type) {

        Map<String, Object> configDataMap = new HashMap<>();
        File file = new File(STACK_CACHE_DIR + CONFIGURATIONS_INFO);
        try {
            if (file.exists()) {
                Map<String, Map<String, Object>> configJson = JsonUtils.readFromFile(file, new TypeReference<>() {
                });
                Object configData = configJson.getOrDefault(service, new HashMap<>()).get(type);
                if (configData != null) {
                    configDataMap = JsonUtils.readFromString((String) configData, new TypeReference<>() {
                    });
                }
            }
        } catch (Exception e) {
            log.warn("{} parse error, ", CONFIGURATIONS_INFO, e);
        }

        return configDataMap;
    }

    public static List<String> hosts(String componentName) {
        return hosts().getOrDefault(componentName, List.of());
    }

    public static Map<String, List<String>> hosts() {

        Map<String, List<String>> hostJson = new HashMap<>();
        File file = new File(STACK_CACHE_DIR + HOSTS_INFO);
        if (file.exists()) {
            hostJson = JsonUtils.readFromFile(file, new TypeReference<>() {
            });
        }
        return hostJson;
    }

    public static Map<String, Object> basicInfo() {

        Map<String, Object> settings = new HashMap<>();
        File file = new File(STACK_CACHE_DIR + SETTINGS_INFO);
        if (file.exists()) {
            settings = JsonUtils.readFromFile(file, new TypeReference<>() {
            });
        }
        return settings;
    }

    public static Map<String, Set<String>> users() {

        Map<String, Set<String>> userMap = new HashMap<>();
        File file = new File(STACK_CACHE_DIR + USERS_INFO);
        if (file.exists()) {
            userMap = JsonUtils.readFromFile(file, new TypeReference<>() {
            });
        }
        return userMap;
    }

    public static Set<String> packages() {
        ClusterInfo cluster = cluster();
        return Optional.of(cluster.getPackages()).orElse(Set.of());
    }

    public static List<RepoInfo> repos() {

        List<RepoInfo> repoInfoList = List.of();
        File file = new File(STACK_CACHE_DIR + REPOS_INFO);
        if (file.exists()) {
            repoInfoList = JsonUtils.readFromFile(file, new TypeReference<>() {
            });
        }
        return repoInfoList;
    }

    public static ClusterInfo cluster() {

        ClusterInfo clusterInfo = new ClusterInfo();
        File file = new File(STACK_CACHE_DIR + CLUSTER_INFO);
        if (file.exists()) {
            clusterInfo = JsonUtils.readFromFile(file, new TypeReference<>() {
            });
        }
        return clusterInfo;
    }

    public static Map<String, ComponentInfo> components() {

        Map<String, ComponentInfo> componentInfo = new HashMap<>();
        File file = new File(STACK_CACHE_DIR + COMPONENTS_INFO);
        if (file.exists()) {
            componentInfo = JsonUtils.readFromFile(file, new TypeReference<>() {
            });
        }
        return componentInfo;
    }
}
