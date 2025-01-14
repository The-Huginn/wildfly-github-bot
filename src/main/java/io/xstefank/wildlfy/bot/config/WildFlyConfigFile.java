package io.xstefank.wildlfy.bot.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class WildFlyConfigFile {

    public WildFlyConfig wildfly;

    public static final class WildFlyConfig {

        public List<WildFlyRule> rules;

        public Format format;
    }

    public static final class WildFlyRule {

        public String id;

        public String title;

        public String body;

        public String titleBody;

        @JsonDeserialize(as = TreeSet.class)
        public Set<String> directories = new TreeSet<>();

        @JsonDeserialize(as = TreeSet.class)
        public Set<String> notify = new TreeSet<>();

        @Override
        public String toString() {
            return "id=" + stringify(id) + " title=" + stringify(title) + " body=" + stringify(body) + " titleBody=" + stringify(titleBody);
        }

        private String stringify(String value) {
            return value == null ? "null" : value;
        }
    }
}
