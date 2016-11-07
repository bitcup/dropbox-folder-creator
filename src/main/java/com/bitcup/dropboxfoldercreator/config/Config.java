package com.bitcup.dropboxfoldercreator.config;

import java.util.ResourceBundle;

/**
 * @author bitcup
 */
public class Config {
    private static final Config instance = new Config();

    private final ResourceBundle resourceBundle;

    private Config() {
        resourceBundle = ResourceBundle.getBundle("app");
    }

    public static String val(String key) {
        return instance.resourceBundle.getString(key);
    }

}
