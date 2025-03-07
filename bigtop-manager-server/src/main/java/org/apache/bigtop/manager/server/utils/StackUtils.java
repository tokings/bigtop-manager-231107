package org.apache.bigtop.manager.server.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.bigtop.manager.common.enums.Command;
import org.apache.bigtop.manager.common.utils.JsonUtils;
import org.apache.bigtop.manager.common.utils.YamlUtils;
import org.apache.bigtop.manager.server.exception.ApiException;
import org.apache.bigtop.manager.server.exception.ServerException;
import org.apache.bigtop.manager.server.model.dto.ServiceDTO;
import org.apache.bigtop.manager.server.model.dto.StackDTO;
import org.apache.bigtop.manager.server.model.mapper.ServiceMapper;
import org.apache.bigtop.manager.server.model.mapper.StackMapper;
import org.apache.bigtop.manager.server.stack.dag.ComponentCommandWrapper;
import org.apache.bigtop.manager.server.stack.dag.DAG;
import org.apache.bigtop.manager.server.stack.dag.DagGraphEdge;
import org.apache.bigtop.manager.server.stack.pojo.ServiceModel;
import org.apache.bigtop.manager.server.stack.pojo.StackModel;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StackUtils {

    private static final String ROLE_COMMAND_SPLIT = "-";

    private static final String BIGTOP_MANAGER_STACK_PATH = "bigtop.manager.stack.path";

    private static final String META_FILE = "metainfo.yaml";

    private static final String STACKS_FOLDER_NAME = "stacks";

    private static final String SERVICES_FOLDER_NAME = "services";

    private static final String CONFIGURATION_FOLDER_NAME = "configuration";

    private static final String DEPENDENCY_FILE = "order.json";

    private static final Map<String, Map<String, List<String>>> STACK_DEPENDENCY_MAP = new HashMap<>();

    private static final Map<String, Map<String, Set<String>>> STACK_CONFIG_MAP = new HashMap<>();

    private static final Map<String, ImmutablePair<StackDTO, List<ServiceDTO>>> STACK_KEY_MAP = new HashMap<>();

    private static final Map<String, DAG<String, ComponentCommandWrapper, DagGraphEdge>> STACK_DAG_MAP = new HashMap<>();

    public static Map<String, Map<String, List<String>>> getStackDependencyMap() {
        return Collections.unmodifiableMap(STACK_DEPENDENCY_MAP);
    }

    public static Map<String, Map<String, Set<String>>> getStackConfigMap() {
        return Collections.unmodifiableMap(STACK_CONFIG_MAP);
    }

    public static Map<String, ImmutablePair<StackDTO, List<ServiceDTO>>> getStackKeyMap() {
        return Collections.unmodifiableMap(STACK_KEY_MAP);
    }

    public static Map<String, DAG<String, ComponentCommandWrapper, DagGraphEdge>> getStackDagMap() {
        return Collections.unmodifiableMap(STACK_DAG_MAP);
    }

    /**
     * Parse stack file to generate stack model
     * @return stack model {@link StackModel}
     */
    public static StackDTO parseStack(File versionFolder) {
        return StackMapper.INSTANCE.Model2DTO(YamlUtils.readYaml(
                versionFolder.getAbsolutePath() + File.separator + META_FILE,
                StackModel.class));
    }

    /**
     * Parse service file to generate service model
     * @param fullStackName full stack name
     * @return service model {@link ServiceModel}
     */
    public static List<ServiceDTO> parseService(File versionFolder, String fullStackName) {
        Map<String, Set<String>> mergedConfigMap = new HashMap<>();

        File[] files = new File(versionFolder.getAbsolutePath(), SERVICES_FOLDER_NAME).listFiles();

        List<ServiceDTO> services = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                log.info("service dir: {}", file);

                // metainfo.yaml
                ServiceModel serviceModel = YamlUtils.readYaml(file.getAbsolutePath() + "/" + META_FILE, ServiceModel.class);
                ServiceDTO serviceDTO = ServiceMapper.INSTANCE.Model2DTO(serviceModel);
                services.add(serviceDTO);

                // order.json
                File dependencyFile = new File(file.getAbsolutePath(), DEPENDENCY_FILE);
                if (dependencyFile.exists()) {
                    Map<String, List<String>> dependencyMap = STACK_DEPENDENCY_MAP.computeIfAbsent(fullStackName, k -> new HashMap<>());
                    dependencyMap.putAll(JsonUtils.readFromFile(dependencyFile, new TypeReference<>() {}));
                }

                // configurations
                Set<String> serviceConfigSet = new HashSet<>();
                File configFolder = new File(file.getAbsolutePath(), CONFIGURATION_FOLDER_NAME);
                if (configFolder.exists()) {
                    for (File configFile : Optional.ofNullable(configFolder.listFiles()).orElse(new File[0])) {
                        serviceConfigSet.add(configFile.getAbsolutePath());
                    }
                }

                mergedConfigMap.put(serviceDTO.getServiceName(), serviceConfigSet);
            }

            STACK_CONFIG_MAP.put(fullStackName, mergedConfigMap);
        }

        log.info("Stack config map: {}", STACK_CONFIG_MAP);
        log.info("Stack dependency map: {}", STACK_DEPENDENCY_MAP);
        return services;
    }

    /**
     * @return stack list map
     */
    public static Map<StackDTO, List<ServiceDTO>> stackList() {
        File stacksFolder = loadStacksFolder();
        File[] stackFolders = Optional.ofNullable(stacksFolder.listFiles()).orElse(new File[0]);
        Map<StackDTO, List<ServiceDTO>> stackMap = new HashMap<>();

        for (File stackFolder : stackFolders) {
            String stackName = stackFolder.getName();
            File[] versionFolders = Optional.ofNullable(stackFolder.listFiles()).orElse(new File[0]);

            for (File versionFolder : versionFolders) {
                String stackVersion = versionFolder.getName();
                String fullStackName = fullStackName(stackName, stackVersion);
                log.info("Parsing stack: {}", fullStackName);

                checkStack(versionFolder);
                StackDTO stackDTO = parseStack(versionFolder);
                List<ServiceDTO> services = parseService(versionFolder, fullStackName);

                stackMap.put(stackDTO, services);

                STACK_KEY_MAP.put(fullStackName, new ImmutablePair<>(stackDTO, services));
            }
        }

        initializeDag();
        return stackMap;
    }

    /**
     * Initialize the DAG for each stack
     */
    private static void initializeDag() {
        for (Map.Entry<String, Map<String, List<String>>> mapEntry : StackUtils.getStackDependencyMap().entrySet()) {
            String fullStackName = mapEntry.getKey();
            DAG<String, ComponentCommandWrapper, DagGraphEdge> dag = new DAG<>();

            for (Map.Entry<String, List<String>> entry : mapEntry.getValue().entrySet()) {
                String blocked = entry.getKey();
                List<String> blockers = entry.getValue();
                addToDagNode(dag, blocked);

                for (String blocker : blockers) {
                    addToDagNode(dag, blocker);
                    dag.addEdge(blocker, blocked, new DagGraphEdge(blocker, blocked), false);
                }
            }

            STACK_DAG_MAP.put(fullStackName, dag);
        }
    }

    private static void addToDagNode(DAG<String, ComponentCommandWrapper, DagGraphEdge> dag, String roleCommand) {
        String[] split = roleCommand.split(ROLE_COMMAND_SPLIT);
        String role = split[0];
        String command = split[1];

        if (!EnumUtils.isValidEnum(Command.class, command)) {
            throw new ServerException("Unsupported command: " + command);
        }

        ComponentCommandWrapper commandWrapper = new ComponentCommandWrapper(role, Command.valueOf(command));
        dag.addNodeIfAbsent(roleCommand, commandWrapper);
    }

    /**
     * Load stack folder as file
     */
    private static File loadStacksFolder() throws ApiException {
        String stackPath = System.getProperty(BIGTOP_MANAGER_STACK_PATH);
        stackPath = stackPath == null ? "" : stackPath;

        File file = new File(stackPath);
        if (!file.exists() || !file.isDirectory()) {
            URL url = StackUtils.class.getClassLoader().getResource(STACKS_FOLDER_NAME);
            if (url == null) {
                throw new ServerException("Can't find stack folder");
            }

            stackPath = url.getPath();
            file = new File(stackPath);
            if (!file.exists()) {
                throw new ServerException("Can't find stack folder");
            }
        }

        log.info("stack file: {}", file);
        return file;
    }

    /**
     * Check stack file
     * @param versionFolder stack version folder
     */
    private static void checkStack(File versionFolder) {
        String[] list = versionFolder.list();
        if (list == null || !Arrays.asList(list).contains(META_FILE)) {
            throw new ServerException("Missing metainfo.yaml in stack version folder: " + versionFolder.getAbsolutePath());
        }
    }

    /**
     * Generate full stack name
     * @param stackName BIGTOP
     * @param stackVersion 3.3.0
     * @return {stackName}-{stackVersion} eg. BIGTOP-3.3.0
     */
    public static String fullStackName(String stackName, String stackVersion) {
        return stackName + "-" + stackVersion;
    }

}
