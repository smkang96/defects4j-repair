package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Specialized POJO serializer that differs from
 * {@link com.fasterxml.jackson.databind.ser.BeanSerializer}
 * in that instead of producing a JSON Object it will output
 * a JSON Array, omitting field names, and serializing values in
 * specified serialization order.
 * This behavior is usually triggered by using annotation
 * {@link com.fasterxml.jackson.annotation.JsonFormat} or its
 * equivalents.
 *<p>
 * This serializer can be used for "simple" instances; and will NOT
 * be used if one of following is true:
 *<ul>
 * <li>Unwrapping is used (no way to expand out array in JSON Object)
 *  </li>
 * <li>Type information ("type id") is to be used: while this could work
 *   for some embedding methods, it would likely cause conflicts.
 *  </li>
 * <li>Object Identity ("object id") is used: while references would work,
 *    the problem is inclusion of id itself.
 *  </li>
 *</ul>
 * Note that it is theoretically possible that last 2 issues could be addressed
 * (by reserving room in array, for example); and if so, support improved.
 *<p>
 * In cases where array-based output is not feasible, this serializer
 * can instead delegate to the original Object-based serializer; this
 * is why a reference is retained to the original serializer.
 * 
 * @since 2.1
 */
public class BeanAsArraySerializer
    extends BeanSerializerBase
{
    private static final long serialVersionUID = -893701886577615846L;

    /**
     * Serializer that would produce JSON Object version; used in
     * cases where array output can not be used.
     */
    protected final BeanSerializerBase _defaultSerializer;
    
    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    public BeanAsArraySerializer(BeanSerializerBase src) {    
        super(src, (ObjectIdWriter) null);
        _defaultSerializer = src;
    }

    protected BeanAsArraySerializer(BeanSerializerBase src, String[] toIgnore) {
        super(src, toIgnore);
        _defaultSerializer = src;
    }

    protected BeanAsArraySerializer(BeanSerializerBase src,
            ObjectIdWriter oiw, Object filterId) {
        super(src, oiw, filterId);
        _defaultSerializer = src;
    }
    
    /*
    /**********************************************************
    /* Life-cycle: factory methods, fluent factories
    /**********************************************************
     */

    @Override
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer transformer) {
        /* If this gets called, we will just need delegate to the default
         * serializer, to "undo" as-array serialization
         */
        return _defaultSerializer.unwrappingSerializer(transformer);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return false;
    }

    @Override
    public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
        // can't handle Object Ids, for now, so:
        return _defaultSerializer.withObjectIdWriter(objectIdWriter);
    }

    @Override
    protected BeanSerializerBase withFilterId(Object filterId) {
        return new BeanAsArraySerializer(this, _objectIdWriter, filterId);
    }
    
    @Override
    protected BeanAsArraySerializer withIgnorals(String[] toIgnore) {
        return new BeanAsArraySerializer(this, toIgnore);
    }

    @Override
    protected BeanSerializerBase asArraySerializer() {
        // already is one, so:
        return this;
    }
    
    /*
    /**********************************************************
    /* JsonSerializer implementation that differs between impls
    /**********************************************************
     */

    // Re-defined from base class, due to differing prefixes
    @Override
    public void serializeWithType(Object bean, JsonGenerator gen,
            SerializerProvider provider, TypeSerializer typeSer)
        throws IOException
    {
        /* 10-Dec-2014, tatu: Not sure if this can be made to work reliably;
         *   but for sure delegating to default implementation will not work. So:
         */
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, gen, provider, typeSer);
            return;
        }
        String typeStr = (_typeId == null) ? null : _customTypeId(bean);
        if (typeStr == null) {
            typeSer.writeTypePrefixForArray(bean, gen);
        } else {
            typeSer.writeCustomTypePrefixForArray(bean, gen, typeStr);
        }
        serializeAsArray(bean, gen, provider);
        if (typeStr == null) {
            typeSer.writeTypeSuffixForArray(bean, gen);
        } else {
            typeSer.writeCustomTypeSuffixForArray(bean, gen, typeStr);
        }
    }

    /**
     * Main serialization method that will delegate actual output to
     * configured
     * {@link BeanPropertyWriter} instances.
     */
    @Override
    public final void serialize(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        // [JACKSON-805]
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                && hasSingleElement(provider)) {
            serializeAsArray(bean, gen, provider);
            return;
        }
        /* note: it is assumed here that limitations (type id, object id,
         * any getter, filtering) have already been checked; so code here
         * is trivial.
         */
        gen.writeStartArray();
        // [databind#631]: Assign current value, to be accessible by custom serializers
        gen.setCurrentValue(bean);
        serializeAsArray(bean, gen, provider);
        gen.writeEndArray();
    }

    /*
    /**********************************************************
    /* Field serialization methods
    /**********************************************************
     */

    private boolean hasSingleElement(SerializerProvider provider) {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        return props.length == 1;
    }

    protected final void serializeAsArray(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }

        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop == null) { // can have nulls in filtered list; but if so, MUST write placeholders
                    gen.writeNull();
                } else {
                    prop.serializeAsElement(bean, gen, provider);
                }
            }
            // NOTE: any getters can not be supported either
            //if (_anyGetterWriter != null) {
            //    _anyGetterWriter.getAndSerialize(bean, gen, provider);
            //}
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            JsonMappingException mapE = new JsonMappingException("Infinite recursion (StackOverflowError)", e);
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }
    
    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override public String toString() {
        return "BeanAsArraySerializer for "+handledType().getName();
    }
}
