package com.fasterxml.jackson.module.jaxb.test;

import java.util.*;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;

import com.fasterxml.jackson.module.jaxb.BaseJaxbTest;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * Simple testing that <code>AnnotationIntrospector.Pair</code> works as
 * expected, when used with Jackson and JAXB-based introspector.
 *
 * @author Tatu Saloranta
 */
public class TestIntrospectorPair
    extends BaseJaxbTest
{
    final static AnnotationIntrospector _jacksonAI = new JacksonAnnotationIntrospector();
    final static AnnotationIntrospector _jaxbAI = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
    
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    /**
     * Simple test bean for verifying basic field detection and property
     * naming annotation handling
     */
    @SuppressWarnings("unused")
    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    static class NamedBean
    {
        @JsonProperty
        private String jackson = "1";

        @XmlElement(name="jaxb")
        protected String jaxb = "2";

        @JsonProperty("bothJackson")
        @XmlElement(name="bothJaxb")
        private String bothString = "3";

        public String notAGetter() { return "xyz"; }
    }

    /**
     * Another bean for verifying details of property naming
     */
    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    static class NamedBean2
    {
        @JsonProperty("")
        @XmlElement(name="jaxb")
        public String foo = "abc";

        @JsonProperty("jackson")
        @XmlElement()
        public String getBar() { return "123"; }

        // JAXB, alas, requires setters for all properties too
        public void setBar(String v) { }
    }

    /**
     * And a bean to check how "ignore" annotations work with
     * various combinations of annotation introspectors
     */
    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    static class IgnoreBean
    {
        @JsonIgnore
        public int getNumber() { return 13; }

        @XmlTransient
        public String getText() { return "abc"; }

        public boolean getAny() { return true; }
    }

    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    static class IgnoreFieldBean
    {
        @JsonIgnore public int number = 7;
        @XmlTransient public String text = "123";
        public boolean any = true;
    }

    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    @XmlRootElement(name="test", namespace="urn:whatever")
    static class NamespaceBean
    {
        public String string;
    }

    // Testing [JACKSON-495]
    static class CreatorBean {
        @JsonCreator
        public CreatorBean(@JsonProperty("name") String name) {
            ;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper;
        AnnotationIntrospector pair;
        Map<String,Object> result;

        mapper = new ObjectMapper();
        // first: test with Jackson/Jaxb pair (jackson having precedence)
        pair = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);
        mapper.setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean());
        assertEquals(3, result.size());
        assertEquals("1", result.get("jackson"));
        assertEquals("2", result.get("jaxb"));
        // jackson one should have priority
        assertEquals("3", result.get("bothJackson"));

        mapper = new ObjectMapper();
        pair = new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI);
        mapper.setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean());
        assertEquals(3, result.size());
        assertEquals("1", result.get("jackson"));
        assertEquals("2", result.get("jaxb"));
        // JAXB one should have priority
        assertEquals("3", result.get("bothJaxb"));
    }

    public void testNaming() throws Exception
    {
        ObjectMapper mapper;
        AnnotationIntrospector pair;
        Map<String,Object> result;

        mapper = new ObjectMapper();
        // first: test with Jackson/Jaxb pair (jackson having precedence)
        pair = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);
        mapper.setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean2());
        assertEquals(2, result.size());
        // order shouldn't really matter here...
        assertEquals("123", result.get("jackson"));
        assertEquals("abc", result.get("jaxb"));

        mapper = new ObjectMapper();
        pair = new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI);
        mapper.setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean2());
        /* Hmmh. Not 100% sure what JAXB would dictate.... thus...
         */
        assertEquals(2, result.size());
        assertEquals("abc", result.get("jaxb"));
        //assertEquals("123", result.get("jackson"));
    }

    public void testSimpleIgnore() throws Exception
    {
        // first: only Jackson introspector (default)
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result = writeAndMap(mapper, new IgnoreBean());
        assertEquals(2, result.size());
        assertEquals("abc", result.get("text"));
        assertEquals(Boolean.TRUE, result.get("any"));

        // Then JAXB only
        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(_jaxbAI);

        // jackson one should have priority
        result = writeAndMap(mapper, new IgnoreBean());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(13), result.get("number"));
        assertEquals(Boolean.TRUE, result.get("any"));

        // then both, Jackson first
        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI));

        result = writeAndMap(mapper, new IgnoreBean());
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get("any"));

        // then both, JAXB first
        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI));

        result = writeAndMap(mapper, new IgnoreBean());
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get("any"));
    }

    public void testSimpleFieldIgnore() throws Exception
    {
        ObjectMapper mapper;

        // first: only Jackson introspector (default)
        mapper = new ObjectMapper();
        Map<String,Object> result = writeAndMap(mapper, new IgnoreFieldBean());
        assertEquals(2, result.size());
        assertEquals("123", result.get("text"));
        assertEquals(Boolean.TRUE, result.get("any"));

        // Then JAXB only
        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(_jaxbAI);

        // jackson one should have priority
        result = writeAndMap(mapper, new IgnoreFieldBean());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(7), result.get("number"));
        assertEquals(Boolean.TRUE, result.get("any"));

        // then both, Jackson first
        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI));

        result = writeAndMap(mapper, new IgnoreFieldBean());
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get("any"));

        // then both, JAXB first
        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI));

        result = writeAndMap(mapper, new IgnoreFieldBean());
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get("any"));
    }

    public void testSimpleOther() throws Exception
    {
        // Let's use Jackson+JAXB comb
        AnnotationIntrospector ann = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);

        AnnotatedClass testClass = AnnotatedClass.construct(NamedBean.class, ann, null);
        //assertNull(ann.findSerializationInclusion(testClass, null));

        JavaType type = TypeFactory.defaultInstance().constructType(Object.class);
        assertNull(ann.findDeserializationType(testClass, type));
        assertNull(ann.findDeserializationContentType(testClass, type));
        assertNull(ann.findDeserializationKeyType(testClass, type));
        assertNull(ann.findSerializationType(testClass));

        assertNull(ann.findDeserializer(testClass));
        assertNull(ann.findContentDeserializer(testClass));
        assertNull(ann.findKeyDeserializer(testClass));

        assertFalse(ann.hasCreatorAnnotation(testClass));
    }
    
    public void testRootName() throws Exception
    {
        // first: test with Jackson/Jaxb pair (jackson having precedence)
        AnnotationIntrospector pair = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);
        assertNull(pair.findRootName(AnnotatedClass.construct(NamedBean.class, pair, null)));
        assertEquals("test", pair.findRootName(AnnotatedClass.construct(NamespaceBean.class, pair, null)));

        // then reverse; should make no difference
        pair = new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI);
        assertNull(pair.findRootName(AnnotatedClass.construct(NamedBean.class, pair, null)));
        assertEquals("test", pair.findRootName(AnnotatedClass.construct(NamespaceBean.class, pair, null)));
    }

    /**
     * Test that will just use Jackson annotations, but did trigger [JACKSON-495] due to a bug
     * in JAXB annotation introspector.
     */
    public void testIssue495() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI));
        CreatorBean bean = mapper.readValue("{\"name\":\"foo\"}", CreatorBean.class);
        assertNotNull(bean);
    }
}