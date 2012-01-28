package com.fasterxml.jackson.module.jaxb.deser;

import java.io.IOException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * @author Ryan Heaton
 * @author Tatu Saloranta
 */
@SuppressWarnings("restriction")
public class XmlAdapterJsonDeserializer
    extends StdDeserializer<Object>
{
    protected final static JavaType ADAPTER_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(XmlAdapter.class);

    protected final XmlAdapter<Object,Object> _xmlAdapter;
    protected final JavaType _valueType;

    protected JsonDeserializer<?> _deserializer;
    
    @SuppressWarnings("unchecked")
    public XmlAdapterJsonDeserializer(XmlAdapter<?,?> xmlAdapter)
    {
        super(Object.class); // type not yet known (will be in a second), but that's ok...
        _xmlAdapter = (XmlAdapter<Object,Object>) xmlAdapter;
        // [JACKSON-404] Need to figure out generic type parameters used...
        /* 14-Mar-2011, tatu: This is sub-optimal, as we really should use
         *    configured TypeFactory, not global one; but it should not cause
         *    issues here (issue would be that it will ignore module-provided additional
         *    type manging, most relevant for languages other than Java)
         */
        TypeFactory typeFactory = TypeFactory.defaultInstance();

        JavaType type = typeFactory.constructType(xmlAdapter.getClass());
        JavaType[] rawTypes = typeFactory.findTypeParameters(type, XmlAdapter.class);
        _valueType = (rawTypes == null || rawTypes.length == 0)
            ? TypeFactory.unknownType() : rawTypes[0];
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        /* Unfortunately we can not use the usual resolution mechanism (ResolvableDeserializer)
         * because it won't get called due to way adapters are created. So, need to do it
         * lazily when we get here:
         */
        JsonDeserializer<?> deser = _deserializer;
        if (deser == null) {
            _deserializer = deser = ctxt.findValueDeserializer(_valueType, null);
        }
        Object boundObject = deser.deserialize(jp, ctxt);
        try {
            return _xmlAdapter.unmarshal(boundObject);
        } catch (Exception e) {
            throw new JsonMappingException("Unable to unmarshal (to type "+_valueType+"): "+e.getMessage(), e);
        }
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // Output can be as JSON Object, Array or scalar: no way to know a priori. So:
        return typeDeserializer.deserializeTypedFromAny(jp, ctxt);
    }
}
