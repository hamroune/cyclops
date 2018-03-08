package cyclops.companion.vavr;

import com.oath.anym.AnyMValue;
import com.oath.cyclops.ReactiveConvertableSequence;
import com.oath.cyclops.data.collections.extensions.CollectionX;
import com.oath.cyclops.hkt.Higher;
import com.oath.cyclops.types.traversable.IterableX;
import cyclops.companion.CompletableFutures;
import cyclops.companion.Monoids;
import cyclops.companion.Optionals;
import cyclops.control.Eval;
import cyclops.control.Maybe;
import cyclops.control.Reader;
import cyclops.conversion.vavr.FromCyclops;
import cyclops.conversion.vavr.ToCyclops;
import cyclops.function.Function3;
import cyclops.function.Function4;
import cyclops.function.Monoid;
import cyclops.function.Reducer;
import cyclops.monads.*;
import cyclops.monads.VavrWitness.array;
import cyclops.monads.VavrWitness.either;
import cyclops.monads.VavrWitness.lazy;
import cyclops.monads.Witness.*;
import cyclops.monads.transformers.EitherT;
import cyclops.reactive.ReactiveSeq;
import cyclops.reactive.collections.mutable.ListX;
import cyclops.typeclasses.InstanceDefinitions;
import cyclops.typeclasses.Pure;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.comonad.ComonadByPure;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.monad.*;
import io.vavr.Lazy;
import io.vavr.collection.*;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Utility class for working with Eithers
 *
 * @author johnmcclean
 *
 */
@UtilityClass
public class Eithers {


    public static  <W1 extends WitnessType<W1>,L,T> XorM<W1,either,T> xorM(Either<L,T> type){
        return XorM.right(anyM(type));
    }
    public static  <W1 extends WitnessType<W1>,L,T> XorM<W1,either,T> xorMRight(T type){
        return XorM.right(anyM(Either.right(type)));
    }
    public static  <W1 extends WitnessType<W1>,L,T> XorM<W1,either,T> xorMLeft(L type){
        return XorM.right(anyM(Either.left(type)));
    }
    public static <L, R> Either<L, R> xor(cyclops.control.Either<L, R> value) {

        return value.visit(l -> Either.left(l), r -> Either.right(r));
    }
    public static <T> AnyMValue<either,T> anyM(Either<?,T> either) {
        return AnyM.ofValue(either, VavrWitness.either.INSTANCE);
    }

    public static <L, T, R> Either<L, R> tailRec(T initial, Function<? super T, ? extends Either<L, ? extends Either<T, R>>> fn) {
        Either<L,? extends Either<T, R>> next[] = new Either[1];
        next[0] = Either.right(Either.left(initial));
        boolean cont = true;
        do {
            cont = next[0].fold(__ -> false,p -> p.fold(s -> {
                next[0] = fn.apply(s);
                return true;
            }, pr -> false));
        } while (cont);
        return next[0].map(Either::get);
    }
    public static <L, T, R> Either<L, R> tailRecEither(T initial, Function<? super T, ? extends Either<L, ? extends cyclops.control.Either<T, R>>> fn) {
        Either<L,? extends cyclops.control.Either<T, R>> next[] = new Either[1];
        next[0] = Either.right(cyclops.control.Either.left(initial));
        boolean cont = true;
        do {
            cont = next[0].fold(__ -> false,p -> p.visit(s -> {
                next[0] = fn.apply(s);
                return true;
            }, pr -> false));
        } while (cont);
        return next[0].map(e->e.orElse(null));
    }




    /**
     * Perform a For Comprehension over a Either, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Eithers.
     *
     *  <pre>
     * {@code
     *
     *   import static com.oath.cyclops.reactor.Eithers.forEach4;
     *
    forEach4(Either.just(1),
    a-> Either.just(a+1),
    (a,b) -> Either.<Integer>just(a+b),
    a                  (a,b,c) -> Either.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Either
     * @param value2 Nested Either
     * @param value3 Nested Either
     * @param value4 Nested Either
     * @param yieldingFunction Generates a result per combination
     * @return Either with a combined value generated by the yielding function
     */
    public static <L,T1, T2, T3, R1, R2, R3, R> Either<L,R> forEach4(Either<L,? extends T1> value1,
                                                                 Function<? super T1, ? extends Either<L,R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends Either<L,R2>> value3,
                                                                 Function3<? super T1, ? super R1, ? super R2, ? extends Either<L,R3>> value4,
                                                                 Function4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Either<L,R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Either<L,R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Either<L,R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }


    /**
     * Perform a For Comprehension over a Either, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Eithers.
     *
     *  <pre>
     * {@code
     *
     *   import static com.oath.cyclops.reactor.Eithers.forEach3;
     *
    forEach3(Either.just(1),
    a-> Either.just(a+1),
    (a,b) -> Either.<Integer>just(a+b),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Either
     * @param value2 Nested Either
     * @param value3 Nested Either
     * @param yieldingFunction Generates a result per combination
     * @return Either with a combined value generated by the yielding function
     */
    public static <L,T1, T2, R1, R2, R> Either<L,R> forEach3(Either<L,? extends T1> value1,
                                                         Function<? super T1, ? extends Either<L,R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Either<L,R2>> value3,
                                                         Function3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Either<L,R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Either<L,R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });

    }



    /**
     * Perform a For Comprehension over a Either, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Eithers.
     *
     *  <pre>
     * {@code
     *
     *   import static com.oath.cyclops.reactor.Eithers.forEach;
     *
    forEach(Either.just(1),
    a-> Either.just(a+1),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Either
     * @param value2 Nested Either
     * @param yieldingFunction Generates a result per combination
     * @return Either with a combined value generated by the yielding function
     */
    public static <L,T, R1, R> Either<L,R> forEach2(Either<L,? extends T> value1, Function<? super T, Either<L,R1>> value2,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Either<L,R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
        });



    }






    /**
     * Sequence operation, take a Collection of Eithers and turn it into a Either with a Collection
     * By constrast with {@link Eithers#sequencePresent(Iterable)}, if any Eithers are empty the result
     * is an empty Either
     *
     * <pre>
     * {@code
     *
     *  Either<Integer> just = Either.of(10);
    Either<Integer> none = Either.empty();
     *
     *  Either<ListX<Integer>> opts = Eithers.sequence(ListX.of(just, none, Either.of(1)));
    //Either.empty();
     *
     * }
     * </pre>
     *
     *
     * @param opts Maybes to Sequence
     * @return  Maybe with a List of values
     */
    public static <L,T> Either<L,ListX<T>> sequence(final Iterable<Either<L,T>> opts) {
        java.util.stream.Stream<Either<L, T>> s = ReactiveSeq.fromIterable(opts);
        return sequence(s).map(r ->r.to(ReactiveConvertableSequence::converter).listX());

    }
    /**
     * Sequence operation, take a Collection of Eithers and turn it into a Either with a Collection
     * Only successes are retained. By constrast with {@link Eithers#sequence(Iterable)} Either#empty types are
     * tolerated and ignored.
     *
     * <pre>
     * {@code
     *  Either<Integer> just = Either.of(10);
    Either<Integer> none = Either.empty();
     *
     * Either<ListX<Integer>> maybes = Eithers.sequencePresent(ListX.of(just, none, Either.of(1)));
    //Either.of(ListX.of(10, 1));
     * }
     * </pre>
     *
     * @param opts Eithers to Sequence
     * @return Either with a List of values
     */
    public static <L,T> Either<L,ListX<T>> sequencePresent(final Iterable<Either<L,T>> opts) {
      java.util.stream.Stream<Either<L, T>> s = ReactiveSeq.fromIterable(opts);
        return sequence(s.filter(Either::isRight)).map(r->r.to(ReactiveConvertableSequence::converter).listX());
    }
    /**
     * Sequence operation, take a Collection of Eithers and turn it into a Either with a Collection
     * By constrast with {@link Eithers#sequencePresent(Iterable)} if any Either types are empty
     * the return type will be an empty Either
     *
     * <pre>
     * {@code
     *
     *  Either<Integer> just = Either.of(10);
    Either<Integer> none = Either.empty();
     *
     *  Either<ListX<Integer>> maybes = Eithers.sequence(ListX.of(just, none, Either.of(1)));
    //Either.empty();
     *
     * }
     * </pre>
     *
     *
     * @param opts Maybes to Sequence
     * @return  Either with a List of values
     */
    public static <L,T> Either<L,ReactiveSeq<T>> sequence(final java.util.stream.Stream<Either<L,T>> opts) {
        return AnyM.sequence(opts.map(Eithers::anyM), either.INSTANCE)
                .map(ReactiveSeq::fromStream)
                .to(VavrWitness::either);

    }
    /**
     * Accummulating operation using the supplied Reducer (@see cyclops2.Reducers). A typical use case is to accumulate into a Persistent Collection type.
     * Accumulates the present results, ignores empty Eithers.
     *
     * <pre>
     * {@code
     *  Either<Integer> just = Either.of(10);
    Either<Integer> none = Either.empty();

     * Either<PersistentSetX<Integer>> opts = Either.accumulateJust(ListX.of(just, none, Either.of(1)), Reducers.toPersistentSetX());
    //Either.of(PersistentSetX.of(10, 1)));
     *
     * }
     * </pre>
     *
     * @param eithers Eithers to accumulate
     * @param reducer Reducer to accumulate values with
     * @return Either with reduced value
     */
    public static <T, L,R> Either<L,R> accumulatePresent(final IterableX<Either<L,T>> eithers, final Reducer<R,T> reducer) {
        return sequencePresent(eithers).map(s -> s.mapReduce(reducer));
    }
    /**
     * Accumulate the results only from those Eithers which have a value present, using the supplied mapping function to
     * convert the data from each Either before reducing them using the supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     *
     * <pre>
     * {@code
     *  Either<Integer> just = Either.of(10);
    Either<Integer> none = Either.empty();

     *  Either<String> opts = Either.accumulateJust(ListX.of(just, none, Either.of(1)), i -> "" + i,
    Monoids.stringConcat);
    //Either.of("101")
     *
     * }
     * </pre>
     *
     * @param eithers Eithers to accumulate
     * @param mapper Mapping function to be applied to the result of each Either
     * @param reducer Monoid to combine values from each Either
     * @return Either with reduced value
     */
    public static <T,L, R> Either<L,R> accumulatePresent(final IterableX<Either<L,T>> eithers, final Function<? super T, R> mapper,
                                                     final Monoid<R> reducer) {
        return sequencePresent(eithers).map(s -> s.map(mapper)
                .reduce(reducer));
    }
    /**
     * Accumulate the results only from those Eithers which have a value present, using the
     * supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     *
     * <pre>
     * {@code
     *  Either<Integer> just = Either.of(10);
    Either<Integer> none = Either.empty();

     *  Either<String> opts = Either.accumulateJust(Monoids.stringConcat,ListX.of(just, none, Either.of(1)),
    );
    //Either.of("101")
     *
     * }
     * </pre>
     *
     * @param eitherals Eithers to accumulate
     * @param reducer Monoid to combine values from each Either
     * @return Either with reduced value
     */
    public static <L,T> Either<L,T> accumulatePresent(final Monoid<T> reducer, final Iterable<Either<L,T>> eitherals) {
        return sequencePresent(eitherals).map(s -> s
                .reduce(reducer));
    }


    /**
     * Combine an Either with the provided Either using the supplied BiFunction
     *
     * <pre>
     * {@code
     *  Eithers.combine(Either.of(10),Either.of(20), this::add)
     *  //Either[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     *
     * @param f Either to combine with a value
     * @param v Either to combine
     * @param fn Combining function
     * @return Either combined with supplied value, or empty Either if no value present
     */
    public static <T1, T2, L,R> Either<L,R> zip(final Either<L,? extends T1> f, final Either<L,? extends T2> v,
                                                final BiFunction<? super T1, ? super T2, ? extends R> fn) {
      return narrow(FromCyclops.either(ToCyclops.either(f)
        .zip(ToCyclops.either(v), fn)));
    }



    /**
     * Narrow covariant type parameter
     *
     * @param eitheral Either with covariant type parameter
     * @return Narrowed Either
     */
    public static <L,T> Either<L,T> narrow(final Either<L,? extends T> eitheral) {
        return (Either<L,T>) eitheral;
    }



    public <L,R,W extends WitnessType<W>> EitherT<W, L, R> liftM(Either<L,R> either, W witness) {
        return EitherT.of(witness.adapter().unit(ToCyclops.either(either)));
    }

}
