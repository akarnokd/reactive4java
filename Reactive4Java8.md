

# Introduction #

The new language features of Java 8 make the Reactive4Java library more usable, but unfortunately, it can't exploit all of these features. Therefore, I started reimplementing the library with Java 8 features. To avoid confusion, the new package name is `hu.akarnokd.reactive4java8`, so both versions of the library can be in the same project.

# Details #

The following Java 8 features are extensively used by the framework:

  * **Default functional interfaces**. It is not absolutely necessary but since the new Java 8 functional interfaces have useful default methods, allowing them to be used around the framework.
    * `Action0 -> Runnable`
    * `Action1 -> Consumer`
    * `Action2 -> BiConsumer`
      * `Action2<Integer, T> -> IndexedConsumer`
    * `Func0 -> Supplier`
    * `Func1 -> Function`
      * `Func1<T, Boolean> -> Predicate`
    * `Func2 -> BiFunction`
      * `Func1<Integer, T> -> IndexedFunction`
  * **Static methods on interfaces**. Previously, interfaces could contain only abstract methods and constant values. Now they can have static methods with implementations. Therefore, most previous static utility methods are now moved to each respective base interface where it is logical.
  * **Default methods on interfaces**. Now you can have methods with default implementations inside interfaces. The sole usage here is to emulate C# extension methods; it is unlikely some implementation of `Observable` would override `select()` on it. Therefore, the previous `ObservableBuilder` is now laregely obsolete:
```
    Observable<Integer> o = Observable.from(1, 2, 3, 4, 5).select(v -> v * v).where(v -> v % 2 == 0);
```
  * **AutoCloseable**. The `register()` method on the `Observable` interface and various other methods now return a `Registration` object which basically extends the `AutoCloseable` interface and hides its `throws Exception` on the `close()` method.
  * **Lambda calculus everywhere**. Most operator implementations can make extensive use of lambda functions (inner classes are less required), making the average length of an operation source lines much less.
  * **Effectively final locals**. Usually, there is no explicit need to define a parameter or variable final any more if it is not mutated.
  * **Better type inference**. Most of the time, there is no need to specify the types in a lambda expression and the diamond operator works better as well. The error messages are more readable.

# Current build #

The current build is linked on the project's main page (no maven or such yet).

The current library is build with NetBeans **7.4** and Java 8 **b115**. Lambda support is relatively working in NetBeans and the javac seems to be working correctly. I'm not certain about the binary stability of the produced jar files though, i.e., b116 might not work with class files compiled by b115.