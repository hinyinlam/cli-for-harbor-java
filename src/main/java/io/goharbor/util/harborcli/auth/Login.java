package io.goharbor.util.harborcli.auth;

import picocli.CommandLine;

import java.util.concurrent.Callable;

public class Login implements Callable<Integer> {

    @CommandLine.Option(names = {"-u", "--username"}, description = "Username", usageHelp = true)
    String username;

    @CommandLine.Option(names = {"-p", "--password"}, description = "Password", arity = "0..1", interactive = true, usageHelp = true)
    char[] password;

    @Override
    public Integer call() throws Exception {
        if(password.length==0){
            return -1;
        }
        byte[] bytes = new byte[password.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) password[i];
        }
        System.out.println("Logging in with username: " + username);

        return 0;
    }
}
