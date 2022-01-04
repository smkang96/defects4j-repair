package com.fasterxml.jackson.databind.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestIgnorePropsForSerialization
    extends BaseMapTest
{
    @JsonIgnoreProperties({"b", "c"})
    static class IgnoreSome
    {
        public int a = 3;
        public String b = "x";

        public int getC() { return -6; }
        public String getD() { return "abc"; }
    }

    @SuppressWarnings("serial")
    @JsonIgnoreProperties({"@class"})
    static class MyMap extends HashMap<String,String> { }

    // [JACKSON-787]: allow use of @JsonIgnoreProperties for properties
    static class WrapperWithPropIgnore
    {
        @JsonIgnoreProperties("y")
        public XY value = new XY();
    }
    
    static class XY {
        public int x = 1;
        public int y = 2;
    }

    static class WrapperWithPropIgnore2
    {
        @JsonIgnoreProperties("z")
        public XYZ value = new XYZ();
    }

    @JsonIgnoreProperties({"x"})
    static class XYZ {
        public int x = 1;
        public int y = 2;
        public int z = 3;
    }

    // also ought to work without full typing?
    static class WrapperWithPropIgnoreUntyped
    {
        @JsonIgnoreProperties("y")
        public Object value = new XYZ();
    }

    static class MapWrapper {
        @JsonIgnoreProperties({"a"})
        public final HashMap<String,Integer> value = new HashMap<String,Integer>();
        {
            value.put("a", 1);
            value.put("b", 2);
        }
    }
    
    /*
    /****************************************************************
    /* Unit tests
    /****************************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    public void testExplicitIgnoralWithBean() throws Exception
    {
        IgnoreSome value = new IgnoreSome();
        Map<String,Object> result = writeAndMap(MAPPER, value);
        assertEquals(2, result.size());
        // verify that specified fields are ignored
        assertFalse(result.containsKey("b"));
        assertFalse(result.containsKey("c"));
        // and that others are not
        assertEquals(Integer.valueOf(value.a), result.get("a"));
        assertEquals(value.getD(), result.get("d"));
    }

    public void testExplicitIgnoralWithMap() throws Exception
    {
        // test simulating need to filter out metadata like class name
        MyMap value = new MyMap();
        value.put("a", "b");
        value.put("@class", MyMap.class.getName());
        Map<String,Object> result = writeAndMap(MAPPER, value);
        assertEquals(1, result.size());
        // verify that specified field is ignored
        assertFalse(result.containsKey("@class"));
        // and that others are not
        assertEquals(value.get("a"), result.get("a"));
    }

    public void testIgnoreViaOnlyProps() throws Exception
    {
        assertEquals("{\"value\":{\"x\":1}}",
                MAPPER.writeValueAsString(new WrapperWithPropIgnore()));
    }

    // Also: should be fine even if nominal type is `java.lang.Object`
    public void testIgnoreViaPropForUntyped() throws Exception
    {
        assertEquals("{\"value\":{\"z\":3}}",
                MAPPER.writeValueAsString(new WrapperWithPropIgnoreUntyped()));
    }
    
    public void testIgnoreWithMapProperty() throws Exception
    {
        assertEquals("{\"value\":{\"b\":2}}", MAPPER.writeValueAsString(new MapWrapper()));
    }
    
    public void testIgnoreViaPropsAndClass() throws Exception
    {
        assertEquals("{\"value\":{\"y\":2}}",
                MAPPER.writeValueAsString(new WrapperWithPropIgnore2()));
    }
}
