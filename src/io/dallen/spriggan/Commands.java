/*
 * Copyright 2017 Donovan Allen.
 * 
 * This file is part of Spriggan.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 */
package io.dallen.spriggan;

import static io.dallen.spriggan.Spriggan.fsep;
import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Donovan Allen
 */
public class Commands {

    @CommandHelp(
            usage = "start [name]",
            desc = "Start the given server or the current server is none is provided"
    )
    public static void start(Server s, String[] args) {
        if (!s.isRunning()) {
            s.start();
        } else {
            System.out.println("Server already running");
        }
    }

    @CommandHelp(
            usage = "start [name]",
            desc = "Stop the given server or the current server is none is provided"
    )
    public static void stop(Server s, String[] args) {
        if (s.isRunning()) {
            s.keepAlive(false);
            s.stop();
        } else {
            System.out.println("Server not running");
        }
    }

    @CommandHelp(
            usage = "kill [name]",
            desc = "Kill the given server or the current server is none is provided, forces proc close"
    )
    public static void kill(Server s, String[] args) {
        if (s.isRunning()) {
            s.kill();
        } else {
            System.out.println("Server not running");
        }
    }

    @CommandHelp(
            usage = "restart [name]",
            desc = "Restart the given server or the current server is none is provided"
    )
    public static void restart(Server s, String[] args) {
        if (s.isRunning()) {
            s.keepAlive(true);
            s.stop();
        } else {
            System.out.println("Server not running");
        }
    }

    @CommandHelp(
            usage = "help",
            desc = "Print this message"
    )
    public static void help(String[] args) {
        for (Method m : Commands.class.getDeclaredMethods()) {
            String output = m.getName();
            if (m.isAnnotationPresent(CommandHelp.class)) {
                output += " - " + m.getAnnotation(CommandHelp.class).usage() + ", " + m.getAnnotation(CommandHelp.class).desc();
            }
            System.out.println(output);
        }
    }

    @CommandHelp(
            usage = "create [name]",
            desc = "Setup a new server"
    )
    public static void create(String[] args) {
        if (Server.allServers().containsKey(args[1])) {
            System.out.println("Server already exists");
        } else {
            Server s = new Server(args[1]);
            s.setup();
            Server.add(s);
        }
    }

    @CommandHelp(
            usage = "install [server] [plugin]",
            desc = "Install the plugin on the server"
    )
    public static void install(Server s, String[] args) {
        Plugin p = Spriggan.getPluginController().getPlugin(args[1]);
        if(s.addPlugin(p)){
            System.out.println("Installed " + p.getName() + " on " + s.getName());
        }else{
            System.out.println("Cannot install on running server, " + p.getName() + " on " + s.getName() + " on next restart");
        }
    }
    
    @CommandHelp(
            usage = "uninstall [server] [plugin]",
            desc = "Uninstall the plugin on the server"
    )
    public static void uninstall(Server s, String[] args) {
        Plugin p = Spriggan.getPluginController().getPlugin(args[1]);
        s.removePlugin(p);
        System.out.println("Removed " + p.getName() + " from " + s.getName());
    }

    @CommandHelp(
            usage = "destroy [server]",
            desc = "Delete the server completely"
    )
    public static void destroy(String[] args) {
        if (!Server.allServers().containsKey(args[1])) {
            System.out.println("Server not found");
        } else {
            Server s = Server.getServer(args[1]);
            if (s.isRunning()) {
                s.stop();
            }
            System.out.println(s.getDataDir().getAbsoluteFile());
            s.getDataDir().delete();
        }
    }

    @CommandHelp(
            usage = "disconnect",
            desc = "Disconnect the current server"
    )
    @StrictlyCurrentServer
    public static void disconnect(Server s, String[] args) {
        System.out.println("Disconnecting from " + s.getName());
        Spriggan.setCurrentServer(null);
    }

    @CommandHelp(
            usage = "current",
            desc = "Print current server name"
    )
    @StrictlyCurrentServer
    public static void current(Server s, String[] args) {
        System.out.println("Currently connected to " + s.getName());
    }

    @CommandHelp(
            usage = "connect [server]",
            desc = "Connect to the given server"
    )
    public static void connect(Server s, String[] args) {
        System.out.println("Connecting to " + s.getName());
        Spriggan.setCurrentServer(s);
    }

    @CommandHelp(
            usage = "exec [command]",
            desc = "Execute the command on the current server"
    )
    @StrictlyCurrentServer
    public static void exec(Server s, String[] args) {
        List<String> cmd = new LinkedList<String>(Arrays.asList(args));
        cmd.remove(0);
        s.executeCommand(String.join(" ", cmd.toArray(new CharSequence[]{})));
    }

    @CommandHelp(
            usage = "exit",
            desc = "Exit Spriggan"
    )
    public static void exit(String[] args) {
        System.out.println("Shutting down");
        for (Server s : Server.allServers().values()) {
            if (s.isRunning()) {
                s.stop();
                s.saveConf();
            }
        }
        Spriggan.getPluginController().saveIfDirty();
        System.exit(0);
    }

    @CommandHelp(
            usage = "track [plugin or location] <as [name]>",
            desc = "track a plugin and store it as name"
    )
    public static void track(String[] args) {
        System.out.println("Adding " + args[1]);
        File repo = new File(args[1]);
        boolean maven = repo.getAbsolutePath().startsWith(Spriggan.getMavenFolder().getAbsolutePath());
        if (args.length > 3 && args[2].equalsIgnoreCase("as")) {
            Spriggan.getPluginController().addPlugin(args[3], repo, maven);
        } else {
            Spriggan.getPluginController().addPlugin(repo.getName(), repo, maven);
        }
        Spriggan.getPluginController().saveIfDirty();
    }
    
    @CommandHelp(
            usage = "locate [search term]",
            desc = "locate a plugin by name in the maven repo"
    )
    public static void locate(String[] args) {
        System.out.println("Finding " + args[1]);
        File repo;
        repo = Plugin.searchRepo(args[1]);
        System.out.println(repo.getAbsolutePath());
        ((TermUtil) System.out).setLastOutput(repo.getAbsolutePath());
    }

    @CommandHelp(
            usage = "plugins",
            desc = "list all known plugins and their location"
    )
    public static void plugins(String[] args) {
        for(Map.Entry<String, Plugin> e : Spriggan.getPluginController().getPlugins().entrySet()){
            System.out.println(e.getKey() + " -> " + e.getValue().getLocation().getAbsolutePath());
        }
        if(Spriggan.getPluginController().getPlugins().entrySet().isEmpty()){
            System.out.println("No tracked plugins");
        }
    }
    
    @CommandHelp(
            usage = "plugins",
            desc = "list all known plugins and their location"
    )
    public static void plugins(Server s, String[] args) {
        for(Map.Entry<String, InstalledPlugin> e : s.getPlugins().entrySet()){
            System.out.println(e.getKey() + " -> " + e.getValue().getInstalledLocation().getAbsolutePath());
        }
        if(Spriggan.getPluginController().getPlugins().entrySet().isEmpty()){
            System.out.println("No tracked plugins");
        }
    }

    @CommandHelp(
            usage = "dat [server]",
            desc = "Display the data file for the given server"
    )
    public static void dat(Server s, String[] args) {
        System.out.println(s.getDataDir().getAbsolutePath());
    }

    @CommandHelp(
            usage = "running",
            desc = "Display list of running servers"
    )
    public static void running(String[] args) {
        boolean none = true;
        for (Server s : Server.allServers().values()) {
            if (s.isRunning()) {
                System.out.println(s.getName());
                none = false;
            }
        }
        if(none){
            System.out.println("No running servers");
        }
    }

    @CommandHelp(
            usage = "servers",
            desc = "List all known servers"
    )
    public static void servers(String[] args) {
        if(Server.allServers().isEmpty()){
            System.out.println("No servers found");
        }
        for (Server s : Server.allServers().values()) {
            System.out.println(s.getName());
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface StrictlyCurrentServer {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface CommandHelp {

        String usage();

        String desc();
    }
}
