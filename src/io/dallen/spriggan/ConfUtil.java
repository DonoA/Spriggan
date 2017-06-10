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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Donovan Allen
 */
public class ConfUtil {

    public static Map<String, String> loadConfig(File conf) {
        Map<String, String> data = new HashMap<String, String>();
        try (BufferedReader br = new BufferedReader(new FileReader(conf))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(line.startsWith("#")){
                    continue;
                }
                String[] args = line.split("=");
                data.put(args[0], args[1]);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return data;
    }

    public static void saveConfig(File conf, Map<String, String> data) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(conf))) {
            bw.write("#Written " + new Date().toLocaleString() + "\n");
            for (Entry<String, String> e : data.entrySet()) {
                bw.write(e.getKey() + "=" + e.getValue() + "\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
