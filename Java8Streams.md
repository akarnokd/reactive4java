# Introduction #

Java 8 is planned to have lambdified collections and default methods introduced. This page describes the relevance of Reactive4Java in contrast of these new features.

**Note that as of January 7 the Java 8 lambda stuff is still fluctuating in terms of API, methods, locations, etc.**

Overall, the new Stream API overlaps with the Interactive operators, with the extension that Stream might be processed in parallel. The dual concept, e.g., Reactive operators, are completely absent from Java 8.

# Interoperation #

Reactive4Java is working well with the new lambda syntax, thanks to its careful design. Once Java 8 is API frozen, I will create a branch or helper library to interoperate with Java 8 concepts.

# Details #

## Lambdification ##

The **lambdification** means that many operators and utility classes now receive methods with functional interface parameters that can be used via the new lambda syntax. For example,
```

    List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3 ,4));
    list.removeAll(i -> i % 2 == 0);    

```

where the collection's removeAll method received an overload accepting a predicate.

## Default methods ##

Default methods are introduced to help evolve interfaces without breaking any implementation by having default implementations for the new methods. This somewhat resembles to mixins or traits found in other languages.

Since it requires to have the source code for the interface to add new functionality. This can be overridden in implementation with ease. If you are the owner of the interface, this is only a matter of release frequency. Having Java 8 collection and stream methods backed in such way could be somewhat problematic as you won't be able to add a new method to Iterable on your own.

In contrast the C# extension method lets you do this by using compiler tricks. Its drawback is that these static methods can't be overridden, new behavior needs to be added as a separate class and can't be automatically applied to previous use positions.