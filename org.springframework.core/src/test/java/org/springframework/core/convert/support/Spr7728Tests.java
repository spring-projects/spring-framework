package org.springframework.core.convert.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

public class Spr7728Tests
{
    private CollectionToCollectionConverter theConverter;
    private Vector<String> theSrcVector;
    private TypeDescriptor theTargetType;

    @Before
    public void setup()
    {
        theSrcVector = new Vector<String>();
        theTargetType = TypeDescriptor.forObject(new ArrayList());
        theConverter = new CollectionToCollectionConverter(new GenericConversionService());
    }

    @Test
    public void convertEmptyVector_shouldReturnEmptyArrayList()
        throws Exception
    {
        theSrcVector.add("Element");
        testCollectionConversionToArrayList(theSrcVector);
    }

    @Test
    public void convertNonEmptyVector_shouldReturnNonEmptyArrayList()
        throws Exception
    {
        testCollectionConversionToArrayList(theSrcVector);
    }

    private void testCollectionConversionToArrayList(Collection<String> aSource)
    {
        Object myConverted = theConverter.convert(aSource, TypeDescriptor.forObject(aSource), theTargetType);
        Assert.assertTrue(myConverted instanceof ArrayList<?>);
        Assert.assertEquals(aSource.size(), ((ArrayList<?>) myConverted).size());
    }

}