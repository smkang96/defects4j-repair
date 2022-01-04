/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.cli;

import junit.framework.TestCase;
import junitx.framework.ArrayAssert;

public class ValuesTest extends TestCase
{
    private CommandLine cmd;

    public void setUp() throws Exception
    {
        Options options = new Options();

        options.addOption("a", false, "toggle -a");
        options.addOption("b", true, "set -b");
        options.addOption("c", "c", false, "toggle -c");
        options.addOption("d", "d", true, "set -d");

        options.addOption(OptionBuilder.withLongOpt("e").hasArgs().withDescription("set -e ").create('e'));
        options.addOption("f", "f", false, "jk");
        options.addOption(OptionBuilder.withLongOpt("g").hasArgs(2).withDescription("set -g").create('g'));
        options.addOption(OptionBuilder.withLongOpt("h").hasArg().withDescription("set -h").create('h'));
        options.addOption(OptionBuilder.withLongOpt("i").withDescription("set -i").create('i'));
        options.addOption(OptionBuilder.withLongOpt("j").hasArgs().withDescription("set -j").withValueSeparator('=').create('j'));
        options.addOption(OptionBuilder.withLongOpt("k").hasArgs().withDescription("set -k").withValueSeparator('=').create('k'));
        options.addOption(OptionBuilder.withLongOpt("m").hasArgs().withDescription("set -m").withValueSeparator().create('m'));

        String[] args = new String[] { "-a",
                                       "-b", "foo",
                                       "--c",
                                       "--d", "bar",
                                       "-e", "one", "two",
                                       "-f",
                                       "arg1", "arg2",
                                       "-g", "val1", "val2" , "arg3",
                                       "-h", "val1", "-i",
                                       "-h", "val2",
                                       "-jkey=value",
                                       "-j", "key=value",
                                       "-kkey1=value1", 
                                       "-kkey2=value2",
                                       "-mkey=value"};

        CommandLineParser parser = new PosixParser();

        cmd = parser.parse(options,args);
    }

    public void testShortArgs()
    {
        assertTrue("Option a is not set", cmd.hasOption("a"));
        assertTrue("Option c is not set", cmd.hasOption("c"));

        assertNull(cmd.getOptionValues("a"));
        assertNull(cmd.getOptionValues("c"));
    }

    public void testShortArgsWithValue()
    {
        assertTrue("Option b is not set", cmd.hasOption("b"));
        assertTrue(cmd.getOptionValue("b").equals("foo"));
        assertEquals(1, cmd.getOptionValues("b").length);

        assertTrue("Option d is not set", cmd.hasOption("d"));
        assertTrue(cmd.getOptionValue("d").equals("bar"));
        assertEquals(1, cmd.getOptionValues("d").length);
    }

    public void testMultipleArgValues()
    {
        assertTrue("Option e is not set", cmd.hasOption("e"));
        ArrayAssert.assertEquals(new String[] { "one", "two" }, cmd.getOptionValues("e"));
    }

    public void testTwoArgValues()
    {
        assertTrue("Option g is not set", cmd.hasOption("g"));
        ArrayAssert.assertEquals(new String[] { "val1", "val2" }, cmd.getOptionValues("g"));
    }

    public void testComplexValues()
    {
        assertTrue("Option i is not set", cmd.hasOption("i"));
        assertTrue("Option h is not set", cmd.hasOption("h"));
        ArrayAssert.assertEquals(new String[] { "val1", "val2" }, cmd.getOptionValues("h"));
    }

    public void testExtraArgs()
    {
        ArrayAssert.assertEquals("Extra args", new String[] { "arg1", "arg2", "arg3" }, cmd.getArgs());
    }

    public void testCharSeparator()
    {
        // tests the char methods of CommandLine that delegate to the String methods
        assertTrue("Option j is not set", cmd.hasOption("j"));
        assertTrue("Option j is not set", cmd.hasOption('j'));
        ArrayAssert.assertEquals(new String[] { "key", "value", "key", "value" }, cmd.getOptionValues("j"));
        ArrayAssert.assertEquals(new String[] { "key", "value", "key", "value" }, cmd.getOptionValues('j'));

        assertTrue("Option k is not set", cmd.hasOption("k"));
        assertTrue("Option k is not set", cmd.hasOption('k'));
        ArrayAssert.assertEquals(new String[] { "key1", "value1", "key2", "value2" }, cmd.getOptionValues("k"));
        ArrayAssert.assertEquals(new String[] { "key1", "value1", "key2", "value2" }, cmd.getOptionValues('k'));

        assertTrue("Option m is not set", cmd.hasOption("m"));
        assertTrue("Option m is not set", cmd.hasOption('m'));
        ArrayAssert.assertEquals(new String[] { "key", "value" }, cmd.getOptionValues("m"));
        ArrayAssert.assertEquals(new String[] { "key", "value" }, cmd.getOptionValues('m'));
    }

    /**
     * jkeyes - commented out this test as the new architecture
     * breaks this type of functionality.  I have left the test 
     * here in case I get a brainwave on how to resolve this.
     */
    /*
    public void testGetValue()
    {
        // the 'm' option
        assertTrue( _option.getValues().length == 2 );
        assertEquals( _option.getValue(), "key" );
        assertEquals( _option.getValue( 0 ), "key" );
        assertEquals( _option.getValue( 1 ), "value" );

        try {
            assertEquals( _option.getValue( 2 ), "key" );
            fail( "IndexOutOfBounds not caught" );
        }
        catch( IndexOutOfBoundsException exp ) {
            
        }

        try {
            assertEquals( _option.getValue( -1 ), "key" );
            fail( "IndexOutOfBounds not caught" );
        }
        catch( IndexOutOfBoundsException exp ) {

        }
    }
    */
}
