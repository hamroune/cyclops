package cyclops.companion.vavr;

import com.oath.cyclops.anym.AnyMValue;
import com.oath.cyclops.ReactiveConvertableSequence;
import com.oath.cyclops.data.collections.extensions.CollectionX;
import com.oath.cyclops.react.Status;
import com.oath.cyclops.types.Value;
import cyclops.control.Either;
import cyclops.conversion.vavr.FromCyclops;
import cyclops.conversion.vavr.ToCyclops;
import cyclops.function.Function3;
import cyclops.function.Function4;
import cyclops.function.Monoid;
import cyclops.function.Reducer;
import cyclops.monads.AnyM;
import cyclops.monads.VavrWitness;
import cyclops.monads.VavrWitness.future;
import cyclops.monads.WitnessType;
import cyclops.monads.XorM;
import cyclops.monads.transformers.FutureT;
import cyclops.reactive.ReactiveSeq;
import cyclops.reactive.collections.mutable.ListX;
import io.vavr.concurrent.Future;
import lombok.experimental.UtilityClass;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utilty methods for working with JDK CompletableFutures
 *
 * @author johnmcclean
 *
 */
@UtilityClass
public class Futures {



    public static  <W1 extends WitnessType<W1>,T> XorM<W1,future,T> xorM(Future<T> type){
        return XorM.right(anyM(type));
    }
    public static <T> void subscribe(final Subscriber<? super T> sub, Future<T> f){
         asPublisher(f).subscribe(sub);
    }
    public static <T> Publisher<T> asPublisher(Future<T> f){
        return ToCyclops.future(f);
    }
    public static <T> AnyMValue<future,T> anyM(Future<T> future) {
        return AnyM.ofValue(future, VavrWitness.future.INSTANCE);
    }

    public static <T, R> Future<R> tailRec(T initial, Function<? super T, ? extends Future<  ? extends io.vavr.control.Either<T, R>>> fn) {
        Future<? extends io.vavr.control.Either<T, R>> next[] = new Future[1];
        next[0] = Future.of(()->io.vavr.control.Either.left(initial));
        boolean cont = true;
        do {

            try{
                next[0].get();
            }catch(Throwable t){
                cont= false;
            }
            if(cont) {
                cont =  next[0].get().fold(s -> {
                    next[0] = fn.apply(s);
                    return true;
                }, pr -> false);
            }
        } while (cont);
        return next[0].map(e->e.get());
    }
    public static <L, T, R> Future< R> tailRecEither(T initial, Function<? super T, ? extends Future< ? extends Either<T, R>>> fn) {
        Future<? extends Either<T, R>> next[] = new Future[1];
        next[0] = Future.of(()->Either.left(initial));
        boolean cont = true;
        do {

            try{
              next[0].get();
            }catch(Throwable t){
                cont= false;
            }
            if(cont) {
                cont = next[0].get().visit(s -> {
                    next[0] = fn.apply(s);
                    return true;
                }, pr -> false);
            }
        } while (cont);
        return next[0].map(e->e.orElse(null));
    }

    /**
     * Lifts a vavr Future into a cyclops FutureT monad transformer (involves an observables conversion to
     * cyclops Future types)
     *
     */
    public static <T,W extends WitnessType<W>> FutureT<W, T> liftM(Future<T> opt, W witness) {
        return FutureT.of(witness.adapter().unit(ToCyclops.future(opt)));
    }



    /**
     * Select the first Future to complete
     *
     * @see CompletableFuture#anyOf(CompletableFuture...)
     * @param fts FutureWs to race
     * @return First Future to complete
     */
    public static <T> Future<T> anyOf(Future<T>... fts) {
        return FromCyclops.future(cyclops.control.Future.anyOf(ToCyclops.futures(fts)));

    }
    /**
     * Wait until all the provided Future's to complete
     *
     * @see CompletableFuture#allOf(CompletableFuture...)
     *
     * @param fts FutureWs to  wait on
     * @return Future that completes when all the provided Futures Complete. Empty Future result, or holds an Exception
     *         from a provided Future that failed.
     */
    public static <T> Future<T> allOf(Future<T>... fts) {

        return FromCyclops.future(cyclops.control.Future.allOf(ToCyclops.futures(fts)));
    }
    /**
     * Block until a Quorum of results have returned as determined by the provided Predicate
     *
     * <pre>
     * {@code
     *
     * Future<ListX<Integer>> strings = Future.quorum(status -> status.getCompleted() >0, Future.ofSupplier(()->1),Future.future(),Future.future());


    strings.get().size()
    //1
     *
     * }
     * </pre>
     *
     *
     * @param breakout Predicate that determines whether the block should be
     *            continued or removed
     * @param fts FutureWs to  wait on results from
     * @param errorHandler Consumer to handle any exceptions thrown
     * @return Future which will be populated with a Quorum of results
     */
    @SafeVarargs
    public static <T> Future<ListX<T>> quorum(Predicate<Status<T>> breakout, Consumer<Throwable> errorHandler, Future<T>... fts) {

        return FromCyclops.future(cyclops.companion.Futures.quorum(breakout,errorHandler, ToCyclops.futures(fts)));


    }
    /**
     * Block until a Quorum of results have returned as determined by the provided Predicate
     *
     * <pre>
     * {@code
     *
     * Future<ListX<Integer>> strings = Future.quorum(status -> status.getCompleted() >0, Future.ofSupplier(()->1),Future.future(),Future.future());


    strings.get().size()
    //1
     *
     * }
     * </pre>
     *
     *
     * @param breakout Predicate that determines whether the block should be
     *            continued or removed
     * @param fts FutureWs to  wait on results from
     * @return Future which will be populated with a Quorum of results
     */
    @SafeVarargs
    public static <T> Future<ListX<T>> quorum(Predicate<Status<T>> breakout, Future<T>... fts) {

        return FromCyclops.future(cyclops.companion.Futures.quorum(breakout, ToCyclops.futures(fts)));


    }
    /**
     * Select the first Future to return with a successful result
     *
     * <pre>
     * {@code
     * Future<Integer> ft = Future.future();
    Future<Integer> result = Future.firstSuccess(Future.ofSupplier(()->1),ft);

    ft.complete(10);
    result.get() //1
     * }
     * </pre>
     *
     * @param fts Futures to race
     * @return First Future to return with a result
     */
    @SafeVarargs
    public static <T> Future<T> firstSuccess(Future<T>... fts) {
        return FromCyclops.future(cyclops.control.Future.firstSuccess(ToCyclops.futures(fts)));

    }

    /**
     * Perform a For Comprehension over a Future, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Futures.
     *
     *  <pre>
     * {@code
     *
     *   import static com.oath.cyclops.reactor.Futures.forEach4;
     *
    forEach4(Future.just(1),
    a-> Future.just(a+1),
    (a,b) -> Future.<Integer>just(a+b),
    a                  (a,b,c) -> Future.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Future
     * @param value2 Nested Future
     * @param value3 Nested Future
     * @param value4 Nested Future
     * @param yieldingFunction Generates a result per combination
     * @return Future with a combined value generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Future<R> forEach4(Future<? extends T1> value1,
                                                                 Function<? super T1, ? extends Future<R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends Future<R2>> value3,
                                                                 Function3<? super T1, ? super R1, ? super R2, ? extends Future<R3>> value4,
                                                                 Function4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Future<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Future<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Future<R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     *
     * Perform a For Comprehension over a Future, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Futures.
     *
     * <pre>
     * {@code
     *
     *  import static com.oath.cyclops.reactor.Futures.forEach4;
     *
     *  forEach4(Future.just(1),
    a-> Future.just(a+1),
    (a,b) -> Future.<Integer>just(a+b),
    (a,b,c) -> Future.<Integer>just(a+b+c),
    (a,b,c,d) -> a+b+c+d <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Future
     * @param value2 Nested Future
     * @param value3 Nested Future
     * @param value4 Nested Future
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Future with a combined value generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Future<R> forEach4(Future<? extends T1> value1,
                                                                 Function<? super T1, ? extends Future<R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends Future<R2>> value3,
                                                                 Function3<? super T1, ? super R1, ? super R2, ? extends Future<R3>> value4,
                                                                 Function4<? super T1, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
                                                                 Function4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Future<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Future<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Future<R3> c = value4.apply(in,ina,inb);
                    return c.filter(in2->filterFunction.apply(in,ina,inb,in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     * Perform a For Comprehension over a Future, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Futures.
     *
     *  <pre>
     * {@code
     *
     *   import static com.oath.cyclops.reactor.Futures.forEach3;
     *
    forEach3(Future.just(1),
    a-> Future.just(a+1),
    (a,b) -> Future.<Integer>just(a+b),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Future
     * @param value2 Nested Future
     * @param value3 Nested Future
     * @param yieldingFunction Generates a result per combination
     * @return Future with a combined value generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Future<R> forEach3(Future<? extends T1> value1,
                                                         Function<? super T1, ? extends Future<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Future<R2>> value3,
                                                         Function3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Future<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Future<R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });

    }

    /**
     *
     * Perform a For Comprehension over a Future, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Futures.
     *
     * <pre>
     * {@code
     *
     *  import static com.oath.cyclops.reactor.Futures.forEach3;
     *
     *  forEach3(Future.just(1),
    a-> Future.just(a+1),
    (a,b) -> Future.<Integer>just(a+b),
    (a,b,c) -> a+b+c <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Future
     * @param value2 Nested Future
     * @param value3 Nested Future
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Future with a combined value generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Future<R> forEach3(Future<? extends T1> value1,
                                                         Function<? super T1, ? extends Future<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Future<R2>> value3,
                                                         Function3<? super T1, ? super R1, ? super R2, Boolean> filterFunction,
                                                         Function3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Future<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Future<R2> b = value3.apply(in,ina);
                return b.filter(in2->filterFunction.apply(in,ina,in2))
                        .map(in2 -> yieldingFunction.apply(in, ina, in2));
            });



        });

    }

    /**
     * Perform a For Comprehension over a Future, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Futures.
     *
     *  <pre>
     * {@code
     *
     *   import static com.oath.cyclops.reactor.Futures.forEach;
     *
    forEach(Future.just(1),
    a-> Future.just(a+1),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Future
     * @param value2 Nested Future
     * @param yieldingFunction Generates a result per combination
     * @return Future with a combined value generated by the yielding function
     */
    public static <T, R1, R> Future<R> forEach2(Future<? extends T> value1, Function<? super T, Future<R1>> value2,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Future<R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
        });



    }

    /**
     *
     * Perform a For Comprehension over a Future, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Futures.
     *
     * <pre>
     * {@code
     *
     *  import static com.oath.cyclops.reactor.Futures.forEach;
     *
     *  forEach(Future.just(1),
    a-> Future.just(a+1),
    (a,b) -> Future.<Integer>just(a+b),
    (a,b,c) -> a+b+c <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Future
     * @param value2 Nested Future
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Future with a combined value generated by the yielding function
     */
    public static <T, R1, R> Future<R> forEach2(Future<? extends T> value1, Function<? super T, ? extends Future<R1>> value2,
                                                BiFunction<? super T, ? super R1, Boolean> filterFunction,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Future<R1> a = value2.apply(in);
            return a.filter(in2->filterFunction.apply(in,in2))
                    .map(in2 -> yieldingFunction.apply(in,  in2));
        });




    }


    /**
     * Sequence operation, take a Collection of Futures and turn it into a Future with a Collection
     * By constrast with {@link Futures#sequencePresent(Iterable)}, if any Futures are empty the result
     * is an empty Future
     *
     * <pre>
     * {@code
     *
     *  Future<Integer> just = Future.of(10);
    Future<Integer> none = Future.empty();
     *
     *  Future<ListX<Integer>> opts = Futures.sequence(ListX.of(just, none, Future.of(1)));
    //Future.empty();
     *
     * }
     * </pre>
     *
     *
     * @param opts Maybes to Sequence
     * @return  Maybe with a List of values
     */
    public static <T> Future<ListX<T>> sequence(final Iterable<Future<T>> opts) {
        return sequence(cyclops.companion.Streams.stream(opts)).map(s -> s.to(ReactiveConvertableSequence::converter).listX());

    }
    /**
     * Sequence operation, take a Collection of Futures and turn it into a Future with a Collection
     * Only successes are retained. By constrast with {@link Futures#sequence(Iterable)} Future#empty types are
     * tolerated and ignored.
     *
     * <pre>
     * {@code
     *  Future<Integer> just = Future.of(10);
    Future<Integer> none = Future.empty();
     *
     * Future<ListX<Integer>> maybes = Futures.sequencePresent(ListX.of(just, none, Future.of(1)));
    //Future.of(ListX.of(10, 1));
     * }
     * </pre>
     *
     * @param opts Futures to Sequence
     * @return Future with a List of values
     */
    public static <T> Future<ListX<T>> sequencePresent(final Iterable<Future<T>> opts) {
        return sequence(cyclops.companion.Streams.stream(opts).filter(Future::isCompleted)).map(s->s.to(ReactiveConvertableSequence::converter).listX());
    }
    /**
     * Sequence operation, take a Collection of Futures and turn it into a Future with a Collection
     * By constrast with {@link Futures#sequencePresent(Iterable)} if any Future types are empty
     * the return type will be an empty Future
     *
     * <pre>
     * {@code
     *
     *  Future<Integer> just = Future.of(10);
    Future<Integer> none = Future.empty();
     *
     *  Future<ListX<Integer>> maybes = Futures.sequence(ListX.of(just, none, Future.of(1)));
    //Future.empty();
     *
     * }
     * </pre>
     *
     *
     * @param opts Maybes to Sequence
     * @return  Future with a List of values
     */
    public static <T> Future<ReactiveSeq<T>> sequence(final java.util.stream.Stream<Future<T>> opts) {
        return AnyM.sequence(opts.map(Futures::anyM), future.INSTANCE)
                .map(ReactiveSeq::fromStream)
                .to(VavrWitness::future);

    }
    /**
     * Accummulating operation using the supplied Reducer (@see cyclops2.Reducers). A typical use case is to accumulate into a Persistent Collection type.
     * Accumulates the present results, ignores empty Futures.
     *
     * <pre>
     * {@code
     *  Future<Integer> just = Future.of(10);
    Future<Integer> none = Future.empty();

     * Future<PersistentSetX<Integer>> opts = Future.accumulateJust(ListX.of(just, none, Future.of(1)), Reducers.toPersistentSetX());
    //Future.of(PersistentSetX.of(10, 1)));
     *
     * }
     * </pre>
     *
     * @param futureals Futures to accumulate
     * @param reducer Reducer to accumulate values with
     * @return Future with reduced value
     */
    public static <T, R> Future<R> accumulatePresent(final Iterable<Future<T>> futureals, final Reducer<R,T> reducer) {
        return sequencePresent(futureals).map(s -> s.mapReduce(reducer));
    }
    /**
     * Accumulate the results only from those Futures which have a value present, using the supplied mapping function to
     * convert the data from each Future before reducing them using the supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     *
     * <pre>
     * {@code
     *  Future<Integer> just = Future.of(10);
    Future<Integer> none = Future.empty();

     *  Future<String> opts = Future.accumulateJust(ListX.of(just, none, Future.of(1)), i -> "" + i,
    Monoids.stringConcat);
    //Future.of("101")
     *
     * }
     * </pre>
     *
     * @param futureals Futures to accumulate
     * @param mapper Mapping function to be applied to the result of each Future
     * @param reducer Monoid to combine values from each Future
     * @return Future with reduced value
     */
    public static <T, R> Future<R> accumulatePresent(final Iterable<Future<T>> futureals, final Function<? super T, R> mapper,
                                                     final Monoid<R> reducer) {
        return sequencePresent(futureals).map(s -> s.map(mapper)
                .reduce(reducer));
    }
    /**
     * Accumulate the results only from those Futures which have a value present, using the
     * supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     *
     * <pre>
     * {@code
     *  Future<Integer> just = Future.of(10);
    Future<Integer> none = Future.empty();

     *  Future<String> opts = Future.accumulateJust(Monoids.stringConcat,ListX.of(just, none, Future.of(1)),
    );
    //Future.of("101")
     *
     * }
     * </pre>
     *
     * @param futureals Futures to accumulate
     * @param reducer Monoid to combine values from each Future
     * @return Future with reduced value
     */
    public static <T> Future<T> accumulatePresent(final Monoid<T> reducer, final Iterable<Future<T>> futureals) {
        return sequencePresent(futureals).map(s -> s
                .reduce(reducer));
    }

    /**
     * Combine an Future with the provided value using the supplied BiFunction
     *
     * <pre>
     * {@code
     *  Futures.combine(Future.of(10),Maybe.just(20), this::add)
     *  //Future[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     * @param f Future to combine with a value
     * @param v Value to combine
     * @param fn Combining function
     * @return Future combined with supplied value
     */
    public static <T1, T2, R> Future<R> combine(final Future<? extends T1> f, final Value<? extends T2> v,
                                                final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclops.future(ToCyclops.future(f)
                .zip(v, fn)));
    }
    /**
     * Combine an Future with the provided Future using the supplied BiFunction
     *
     * <pre>
     * {@code
     *  Futures.combine(Future.of(10),Future.of(20), this::add)
     *  //Future[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     *
     * @param f Future to combine with a value
     * @param v Future to combine
     * @param fn Combining function
     * @return Future combined with supplied value, or empty Future if no value present
     */
    public static <T1, T2, R> Future<R> combine(final Future<? extends T1> f, final Future<? extends T2> v,
                                                final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return combine(f, ToCyclops.future(v),fn);
    }

    /**
     * Combine an Future with the provided Iterable (selecting one element if present) using the supplied BiFunction
     * <pre>
     * {@code
     *  Futures.zip(Future.of(10),Arrays.asList(20), this::add)
     *  //Future[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     * @param f Future to combine with first element in Iterable (if present)
     * @param v Iterable to combine
     * @param fn Combining function
     * @return Future combined with supplied Iterable, or empty Future if no value present
     */
    public static <T1, T2, R> Future<R> zip(final Future<? extends T1> f, final Iterable<? extends T2> v,
                                            final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclops.future(ToCyclops.future(f)
                .zip(v, fn)));
    }

    /**
     * Combine an Future with the provided Publisher (selecting one element if present) using the supplied BiFunction
     * <pre>
     * {@code
     *  Futures.zip(Flux.just(10),Future.of(10), this::add)
     *  //Future[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     *
     * @param p Publisher to combine
     * @param f  Future to combine with
     * @param fn Combining function
     * @return Future combined with supplied Publisher, or empty Future if no value present
     */
    public static <T1, T2, R> Future<R> zip(final Publisher<? extends T2> p, final Future<? extends T1> f,
                                            final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclops.future(ToCyclops.future(f)
                .zip(fn,p)));
    }
    /**
     * Narrow covariant type parameter
     *
     * @param futureal Future with covariant type parameter
     * @return Narrowed Future
     */
    public static <T> Future<T> narrow(final Future<? extends T> futureal) {
        return (Future<T>) futureal;
    }



}
