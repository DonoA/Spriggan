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

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 *
 * @author Donovan Allen
 */
public class TermUtil extends PrintStream {

    private PrintStream sysOut;
    
    private String lastOutput;
    
    public TermUtil(PrintStream orig) throws FileNotFoundException {
        super("SprigganOut");
        this.sysOut = orig;
    }

    @Override
    public void println(String m) {
        if (System.getenv("COLUMNS") != null) {
            int w = Integer.parseInt(System.getenv("COLUMNS"));
            for (int i = 0; i < w; i++) {
                sysOut.print("\b");
            }
            sysOut.print(m + "\n> ");
        } else {
            sysOut.print("\b\b" + m + "\n> ");
        }
    }
    
    public void setLastOutput(String str){
        this.lastOutput = str;
    }
    
    public String getLastOutput(){
        return this.lastOutput;
    }

    @Override
    public void print(String m) {
        sysOut.print(m);
    }

}
