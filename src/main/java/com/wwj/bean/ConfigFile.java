package com.wwj.bean;

import java.io.Serializable;

public class ConfigFile implements Serializable {

    private static final long serialversionuid = 1L;

    private String dataId;

    private String group;

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
