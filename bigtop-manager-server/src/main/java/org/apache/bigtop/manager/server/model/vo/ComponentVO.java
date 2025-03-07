package org.apache.bigtop.manager.server.model.vo;

import lombok.Data;

@Data
public class ComponentVO {

    private Long id;

    private String componentName;

    private String displayName;

    private String category;

    private String serviceName;

    private String clusterName;

}
