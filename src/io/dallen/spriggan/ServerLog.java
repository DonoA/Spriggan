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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Donovan Allen
 */
public class ServerLog {

    private List<String> output = new LinkedList<String>();

    private final String name;

    private final ServerLogHandler logEvent;

    public ServerLog(String name, ServerLogHandler logEvent) {
        this.logEvent = logEvent;
        this.name = name;
    }

    public void write(String msg) {
        msg = "[" + name + "] " + msg;
        output.add(msg);
        logEvent.write(msg);
    }

    public List<String> getLogs() {
        return output;
    }

    public static interface ServerLogHandler {

        public void write(String msg);
    }
}
