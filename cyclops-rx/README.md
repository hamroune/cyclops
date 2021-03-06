# RxJava Integration


## Get cyclops-rx-integration


* [![Maven Central : cyclops-rx](https://maven-badges.herokuapp.com/maven-central/com.oath.cyclops/cyclops-rx-integration/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.aol.cyclops/cyclops-rx)   [![javadoc.io](https://javadocio-badges.herokuapp.com/com.aol.cyclops/cyclops-rx/badge.svg)](https://javadocio-badges.herokuapp.com/com.aol.cyclops/cyclops-rx)
* [Javadoc for cyclops-rx](http://www.javadoc.io/doc/com.oath.cyclops/cyclops-rx-integration)


# cyclops-rx features include

* Native For Comprehensions
* Observable based ReactiveSeq implementation
  * Implement an extended Java 8 Stream using Rx Observable
  * Full integration with cyclops-react Xtended collections
  * Asynchronously populate an Xtended Collection with Rx Observables, materialize / block on first access
* AnyM monad wrapper for Monadic types (with full integration with cyclops-react Monad abstractions such as Kleisli)
    * Monad wrapper uses native Observable operators
    * Xtended Collections backed by Observable operate on Observable directly
* StreamT monad transformer operates directly with Observable    
* Extension Operators for Observable and ReactiveSeq (extend ReactiveSeq with Observable and Observable with ReactiveSeq)
* Companion classes for Obserbables offering :
  * For comprehensions
  * Higher Kinded Typeclasses
  * Helper functions for combining / accumulating and zipping values
  

# Reactive Collections!

In the example below we asynchronously populate an Xtended list using an Rx Java Observable. Additional, reactive operations can be performed on the List asynchronously.
The ListX only blocks on first access to the data.

```java
import static cyclops.collections.mutable.ListX.listX;
import static cyclops.companion.rx.Observables.reactiveSeq;
AtomicBoolean complete = new AtomicBoolean(false);


Observable<Integer> async =  Observables.fromStream(Spouts.async(Stream.of(100,200,300), Executors.newFixedThreadPool(1)))
                                                .doOnCompleted(()->complete.set(true));

ListX<Integer> asyncList = listX(reactiveSeq(async))
                                      .map(i->i+1);

System.out.println("Blocked? " + complete.get());

System.out.println("First value is "  + asyncList.get(0));

System.out.println("Completed? " + complete.get());
```
Which will print

```
Blocked? false
First value is 101
Completed? true
```

# For Comprehensions

```java
 Observable<Integer> result = Observables.forEach(Observable.just(10, 20),
                                                   a -> Observable.<Integer> just(a + 10),
                                                   (a, b) -> a + b);

 result.toList()
       .toBlocking()
        .single()
        //[30, 50]

```

# ReactiveSeq integration

Use the Observables companion class to create Observable backed ReactiveSeqs

Create an Observable-backed ReactiveSeq directly or from an Observable
```java
ReactiveSeq<Integer> seq = Observables.just(1,2,3);
ReactiveSeq<Integer> seq2 = Observables.reactiveSeq(Observable.just(1,2,3));
```

With an Observable-back ReactiveSeq we can create Reactive Xtended Collections e.g. an extended j.u.List

```java
import static cyclops.collections.mutable.ListX.listX;
import static cyclops.companion.rx.Observables.reactiveSeq

ListX<Integer> asyncList = listX(reactiveSeq(observable))
                                        .map(i->i+1);
```

Or a reactive Vavr Vector

```java
import static cyclops.collections.vavr.VavrVectorX;
import static cyclops.companion.rx.Observables.reactiveSeq

VectorX<Integer> asyncList = vectorX(reactiveSeq(observable))
                                        .map(i->i+1);


//vector is asynchronously populated by our Observable
//we can continue processing and block on first access or
//unwrap to raw Vavr vector type

asyncList.get(1); //will bock until data is available

//will also block until data is available
Vector<Integer> raw = asyncList.to(VavrConverters::Vector); 


```

Or even a Scala List
```java
import static cyclops.collections.scala.ScalaListX;
import static cyclops.companion.rx.Observables.reactiveSeq

LinkedListX<Integer> asyncList = listX(reactiveSeq(observable))
                                        .map(i->i+1);


//vector is asynchronously populated by our Observable
//we can continue processing and block on first access or
//unwrap to raw Vavr vector type

asyncList.get(1); //will bock until data is available

//will also block until data is available
List<Integer> raw = asyncList.to(ScalaConverters::List); 


```

Use the visit method on ReactiveSeq to pattern match over it's reactive nature

1. Synchronous
2. reactive-streams based async backpressure
3. pure asynchronous execution

For ObservableReactiveSeq the visit method always executes the #3 function

```java

ReactiveSeq<Integer> seq = Observables.just(1,2,3);

String type = seq.visit(sync->"synchronous",rs->"reactive-streams",async->"pure async");
observables

```


## Extension operators

Use RxJava to extend cyclops-react's array of operations

```java
import static cyclops.streams.Rx2Operators.observable;

ReactiveSeq<List<Integer>> seq = Observables.of(1,2,3)
                                            .map(i->i+1)
                                            .to(observable(o->o.buffer(10)));
```

Use custom Rx Operators

```java
import static cyclops.streams.Rx2Operators.observable;

ReactiveSeq<List<Integer>> seq = Observables.of(1,2,3)
                                            .to(lift(new Observable.Operator<Integer,Integer>(){
                                                    @Override
                                                    public Subscriber<? super Integer> call(Subscriber<? super Integer> subscriber) {
                                                          return subscriber; // operator code
                                                    }
                                               }))
                                            .map(i->i+1)
```

# AnyM monad abstraction

AnyM is a type that can represent any Java Monad (allowing us to write more abstract code). 

There are three types. AnyM abstracts over all monadic types. AnyMValue represents Monad types that resolve to a single scalar value, AnyMSeq represents monad types that are sequences of values (just like Observable)
```

                        AnyM
                         |
                         |
              __________________________
             |                          |
          AnyMValue                  AnyMSeq    
                                                  
```

We can create an AnyM instance for an Observable via Observables

```java
Observable<Integer> myObservable;
AnyMSeq<obsvervable,Integer> monad = Observables.anyM(myObsevable);

monad.map(i->i*2)
     .zipWithIndex();
     .filter(t->t._1()<100l);
```

Convert back to Observable via Observables.raw (or RxWitness.observable)

```java
AnyMSeq<obsvervable,Integer> monad;
Observable<Integer> obs = Observables.raw(monad);
```


We can write generic methods that accept any Monad type

```java
public <W extends WitnessType<W>> AnyMSeq<W,Integer> sumAdjacent(AnyMSeq<W,Integer> sequence){
     return sequence.sliding(1)
                    .map(t->t.sum(i->i).get())
}
```

AnyM manages your Observables, they still behave reactively like Observables should!

```java
AtomicBoolean complete = new AtomicBoolean(false);

ReactiveSeq<Integer> asyncSeq = Spouts.async(Stream.of(1, 2, 3), Executors.newFixedThreadPool(1));
Observable<Integer> observableAsync = Observables.observableFrom(asyncSeq);
AnyMSeq<obsvervable,Integer> monad = Observables.anyM(observableAsync);

monad.map(i->i*2)
     .forEach(System.out::println,System.err::println,()->complete.set(true));

System.out.println("Blocked? " + complete.get());
while(!complete.get()){
        Thread.yield();
}
```

```
Blocked? false
2
4
6
```

Observables can also be defined as part of the reactiveSeq family of types inside AnyM - ```AnyM<reactiveSeq,Integer>``` 
```java
AnyM<reactiveSeq,Integer> anyM = Observables.just(1,2,3)
                                            .anyM();

ReactiveSeq<Integer> seq = Witness.reactiveSeq(anyM);


```

# StreamT monad transformer

Monad Transformers allow us to manipulate nested types - for example we could use the StreamT monad Transformer to manipulate a List of Observables as if it was a single Obsevable. Or an Observable inside an Optional as if we were operating directly on the Obsevable within.

## Creating StreamT

Via liftM in Observables
```java
ListX<Observables<Integer>> nested = ListX.of(Observable.just(10,20));
StreamT<list,Integer> listOfObservables = Observables.liftM(nested.anyM());
StreamT<list,Integer> doubled = listOfObservables.map(i->i*2);
```

Via Observable backed ReactiveSeq

```java
ReactiveSeq<Integer> reactive = Observables.just(1,2,3);
StreamT<optional,Integer> transformer = reactive.liftM(Witness.optional.INSTANCE);
```

Extacting Observable from StreamT

Use the unwrapTo method in conjunction with Observables::fromStream to get an 
```java 
StreamT<list,Integer> trans = Observables.just(1,2,3).liftM(list.INSTANCE);

AnyM<list,Observable<T>> anyM = trans.unwrapTo(Observables::fromStream);
```

Use Witness.list to convert to a List

```java
StreamT<list,Integer> trans = Observables.just(1,2,3).liftM(list.INSTANCE);

ListX<Observable<Integer>> listObs = Witness.list(trans.unwrapTo(Observables::fromStream));
```


## Using Typeclasses

### Directly 

Typeclasses can be used directly (although this results in verbose and somewhat cumbersome code)
e.g. using the Pure and Functor typeclasses for Observable

```java

   Pure<observable> pure = Observables.Instances.unit();
   Functor<observable> functor = Observables.Instances.functor();
        
   ObservableKind<Integer> flux = pure.unit("hello")
                                      .applyHKT(h->functor.map((String v) ->v.length(), h))
                                      .convert(ObservableKind::narrowK);

        
   assertThat(list.toList().blockingGet(),equalTo(Arrays.asList("hello".length())));
```

### Via Active

The Active class represents a Higher Kinded encoding of a RxJava (or cyclops-react/ JDK/ reactor/ Vavr etc) type *and* it's associated type classes

The code above which creates a new Observable containing a single element "hello" and transforms it to a Flux of Integers (the length of each word), can be written much more succintly with Active

```java

Active<observable,Integer> active = Observables.allTypeClasses(Flux.empty());

Active<observable,Integer> hello = active.unit("hello")
                                         .map(String::length);

Observable<Integer> stream = ObservableKind.narrow(hello.getActive());

```

### Via Nested

The Nested class represents a Nested data structure, for example a Vavr Option with a Observable *and* the associated typeclass instances for both types.

```java
import cyclops.companion.vavr.Options.OptionNested;

Nested<option,observable,Integer> optionObs  = OptionNested.list(Option.some(Observable.just(1,10,2,3)))
                                                                       .map(i -> i * 20);

Option<Integer> opt  = optionObs.foldsUnsafe()
                                .foldLeft(Monoids.intMax)
                                .convert(OptionKind::narrowK);


//[200]

```

### Via Coproduct

Coproduct is a Sum type for HKT encoded types that also stores the associated type classes

```java
import static 
Coproduct<observable,option,Integer> nums = Observables.coproduct(Observables.Instances.definitions(),10);


int value = nums.map(i->i*2)
                .foldUnsafe()
                .foldLeft(0,(a,b)->a+b);

//20

```


# Higher Kinded examples


e.g. using the Pure and Functor typeclasses for Observables

```java

Pure<observable> pure = Observables.Instances.unit();
Functor<observable> functor = Observables.Instances.functor();
        
ObservableKind<Integer> list = pure.unit("hello")
                                   .apply(h->functor.map((String v) ->v.length(), h))
                                  .convert(ObservableKind::narrowK);

        
assertThat(list.toList().toBlocking().first(),equalTo(Arrays.asList("hello".length())));

```


# Direct Higher Kinded Types and Type classes examples


e.g. using the Pure and Functor typeclasses for Vavr Streams

```java

Pure<observable> pure = Observables.Instances.unit();
Functor<observable> functor = Observables.Instances.functor();
        
ObservableKind<Integer> list = pure.unit("hello")
                                   .apply(h->functor.map((String v) ->v.length(), h))
                                  .convert(ObservableKind::narrowK);

        
assertThat(list.toList().toBlocking().first(),equalTo(Arrays.asList("hello".length())));
```

# Kotlin style sequence generators

```java

import static cyclops.stream.Generator.suspend;
import static cyclops.stream.Generator.times;

i = 100;
k = 9999;

Observable<Integer> fi = Observable.from(suspend((Integer i) -> i != 4, s -> {

                         Generator<Integer> gen1 = suspend(times(2), s2 -> s2.yield(i++));
                         Generator<Integer> gen2 = suspend(times(2), s2 -> s2.yield(k--));

                         return s.yieldAll(gen1.stream(), gen2.stream());
                  }
               ));


//Observable(100, 101, 9999, 9998, 102)
```
