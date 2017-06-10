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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 *
 * @author Donovan Allen
 */
public class Commands {

    public static void start(Server s, String[] args) {
        if (!s.isRunning()) {
            s.start();
        } else {
            System.out.println("Server already running");
        }
    }

    public static void stop(Server s, String[] args) {
        if (s.isRunning()) {
            s.keepAlive(false);
            s.stop();
        } else {
            System.out.println("Server not running");
        }
    }

    public static void kill(Server s, String[] args) {
        if (s.isRunning()) {
            s.kill();
        } else {
            System.out.println("Server not running");
        }
    }

    public static void restart(Server s, String[] args) {
        if (s.isRunning()) {
            s.keepAlive(true);
            s.stop();
        } else {
            System.out.println("Server not running");
        }
    }

    public static void help(String[] args) {
        for (Method m : Commands.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
    }

    public static void create(String[] args) {
        if (Server.allServers().containsKey(args[1])) {
            System.out.println("Server already exists");
        } else {
            Server s = new Server(args[1]);
            s.setup();
            Server.add(s);
        }
    }

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

    @StrictlyCurrentServer
    public static void disconnect(Server s, String[] args) {
        System.out.println("Disconnecting from " + s.getName());
        Spriggan.setCurrentServer(null);
    }

    @StrictlyCurrentServer
    public static void current(Server s, String[] args) {
        System.out.println("Currently connected to " + s.getName());
    }

    public static void connect(Server s, String[] args) {
        System.out.println("Connecting to " + s.getName());
        Spriggan.setCurrentServer(s);
    }

    public static void exit(String[] args) {
        System.out.println("Shutting down");
        for (Object b : Server.allServers().values()) {
            Server s = (Server) b;
            if (s.isRunning()) {
                s.stop();
                s.saveConf();
            }
        }
        System.exit(0);
    }

    public static void dat(String[] args) {
        System.out.println(Server.getServer(args[1]).getDataDir().getAbsolutePath());
    }

    public static void running(String[] args) {
        for (Object o : Server.allServers().values()) {
            Server s = (Server) o;
            if (s.isRunning()) {
                System.out.println(s.getName());
            }
        }
    }

    public static void servers(String[] args) {
        for (Object o : Server.allServers().values()) {
            System.out.println(((Server) o).getName());
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface StrictlyCurrentServer {
    }
}
