package io.goharbor.util.harborcli.config;

import lombok.Data;

@Data
public class BasicAuth {
    public String username;
    public String password;
    public String api;
}
