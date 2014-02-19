package com.benstopford.coherence.bootstrap.morecomplex;

import com.benstopford.coherence.bootstrap.structures.dataobjects.ComplexPofObject;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.reflect.PofValue;
import com.tangosol.io.pof.reflect.PofValueParser;
import com.tangosol.io.pof.reflect.SimplePofPath;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.extractor.PofExtractor;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class IsPofAlwaysAGoodIdea extends TestCase {
    public static int objectCount;
    static byte[] padding = new byte[10];
    enum Type {start, end, random};

    /*
    Class to look at performance of pof-extractors in comparison to deserilising the whole object
    I'm using:
         -Xmx8g -Xms8g
         (and tweaking these for fun) -XX:NewSize=5g -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
     */
    public void testStuff() throws InterruptedException {

        padding = new byte[64];
        objectCount = 100000;
        int fieldCount = 50;

        testFullObjectDeserialiation(fieldCount);
        testFullObjectDeserialiation(fieldCount);
        testFullObjectDeserialiation(fieldCount);

        System.gc();
        Thread.sleep(1000);

        testPofExtractionOfNAtrributes(fieldCount, 5, Type.start);
        testPofExtractionOfNAtrributes(fieldCount, 5, Type.start);
        testPofExtractionOfNAtrributes(fieldCount, 5, Type.start);

        System.gc();
        Thread.sleep(1000);

        testPofExtractionOfNAtrributes(fieldCount, 5, Type.end);
        testPofExtractionOfNAtrributes(fieldCount, 5, Type.end);
        testPofExtractionOfNAtrributes(fieldCount, 5, Type.end);

        System.gc();
        Thread.sleep(1000);

        testPofExtractionOfNAtrributes(fieldCount, 5, Type.random);
        testPofExtractionOfNAtrributes(fieldCount, 5, Type.random);
        testPofExtractionOfNAtrributes(fieldCount, 5, Type.random);

        System.gc();
        Thread.sleep(1000); //sanity check same as first results

        testFullObjectDeserialiation(fieldCount);
        testFullObjectDeserialiation(fieldCount);
        testFullObjectDeserialiation(fieldCount);

        System.out.println("----Break Even Points (on my machine) for objects with different numbers of fields----");
        System.out.println("- for large objects of 5 fields the break even point is deserialising 2 fields with pof");
        System.out.println("- for large objects of 20 fields the break even point is deserialising 4 fields with pof");
        System.out.println("- for large objects of 50 fields the break even point is deserialising 5 fields with pof");
        System.out.println("- for large objects of 100 fields the break even point is deserialising 7 fields with pof");
        System.out.println("- for large objects of 200 fields the break even point is deserialising 9 fields with pof");
        System.out.println("----Varying Field Size----");
        System.out.println("- the size of the field (adjusted with fieldPadding) doesn't affect performance much");System.out.println("----Varying Field Size----");
        System.out.println("----Conclusion----");
        System.out.println("Using pof extractors in filters and for indexing is a good idea but if you are doing complex operations in the cache that require access to a broad range of fields it may actually be more efficient to deserialise the whole object");
    }


    public static void  testFullObjectDeserialiation(int numberOfFieldsOnObject) {
        SimplePofContext context = new SimplePofContext();
        List<Binary> data = new ArrayList<Binary>();

        //create a test object
        ComplexPofObject o = createPofObject(numberOfFieldsOnObject);

        //create a 'cache' of n binary versions of that object
        for (int i = 0; i < objectCount; i++) {
            context.registerUserType(2001, ComplexPofObject.class, ComplexPofObject.serializer);
            Binary binary = ExternalizableHelper.toBinary(o, context);
            data.add(binary);
        }

        //deserialise them all
        long start = System.nanoTime();
        for (Binary b : data) {
            ExternalizableHelper.fromBinary(b, context);
        }
        long took = (System.nanoTime() - start);

        System.out.printf("On average full deserialisation of %s field object took %,dns\n", numberOfFieldsOnObject, took / data.size());
    }

    public static void testPofExtractionOfNAtrributes(int numberOfFieldsOnObject, int numberOfFieldsToExract, Type entryPoint) {
        SimplePofContext context = new SimplePofContext();
        List<Binary> data = new ArrayList<Binary>();

        //create a test object
        ComplexPofObject o = createPofObject(numberOfFieldsOnObject);

        //create a 'cache' of n binary versions of that object
        for (int i = 0; i < objectCount; i++) {
            context.registerUserType(2001, ComplexPofObject.class, ComplexPofObject.serializer);
            Binary binary = ExternalizableHelper.toBinary(o, context);
            data.add(binary);
        }

        int[] randomPofIndexes = new int[numberOfFieldsToExract];
        Random random = new Random();
        for(int i =0; i<randomPofIndexes.length; i++){
            randomPofIndexes[i]=random.nextInt(numberOfFieldsOnObject);
        }


        //PofExtract some number of fields from the start/end of stream
        long start = System.nanoTime();
        for (Binary b : data) {
            for (int i = 0; i < numberOfFieldsToExract; i++) {
                if (entryPoint==Type.end) {
                    extract(context, b, numberOfFieldsOnObject - i);
                } else if(entryPoint==Type.start) {
                    extract(context, b, i);
                }else if(entryPoint==Type.random){
                    extract(context, b, randomPofIndexes[i]);
                }
            }
        }
        long took = (System.nanoTime() - start);

        System.out.printf("On average pof extraction of %s %s fields of %s took %,dns\n", entryPoint==Type.end ? "last" : entryPoint==Type.start?"start":"random", numberOfFieldsToExract, numberOfFieldsOnObject, took / data.size());
    }

    private static void extract(SimplePofContext context, Binary b, int index) {
        PofExtractor pofExtractor = new PofExtractor(new SimplePofPath(index));
        PofValue value = PofValueParser.parse(b, context);
        pofExtractor.getNavigator().navigate(value).getValue();
    }

    private static ComplexPofObject createPofObject(int numberOfFields) {
        Object[] fields = new Object[numberOfFields];
        for (int i = 0; i < numberOfFields; i++) {
            fields[i] = String.valueOf(padding) + i;
        }
        return new ComplexPofObject(fields);
    }

}

