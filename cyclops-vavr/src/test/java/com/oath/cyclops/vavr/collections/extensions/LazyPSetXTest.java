package com.oath.cyclops.vavr.collections.extensions;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.oath.cyclops.data.collections.extensions.FluentCollectionX;
import cyclops.collections.vavr.VavrHashSetX;
import cyclops.companion.Reducers;
import cyclops.companion.Semigroups;
import cyclops.control.Option;
import cyclops.data.Vector;
import cyclops.reactive.Streamable;
import cyclops.data.tuple.Tuple2;
import cyclops.reactive.collections.immutable.BagX;
import cyclops.reactive.collections.immutable.PersistentSetX;
import cyclops.reactive.collections.mutable.ListX;
import org.junit.Test;


import reactor.core.publisher.Flux;

public class LazyPSetXTest extends AbstractCollectionXTest  {

    @Override
    public <T> FluentCollectionX<T> of(T... values) {
        PersistentSetX<T> list = VavrHashSetX.empty();
        for (T next : values) {
            list = list.plus(next);
        }
        System.out.println("List " + list);
        return list;

    }


    @Test
    public void sorted() {
        assertThat(of(1,5,3,4,2).sorted().collect(Collectors.toList()).size(),is(Arrays.asList(1,2,3,4,5).size()));
    }
    @Test
    public void streamable(){
        Streamable<Integer> repeat = (of(1,2,3,4,5,6)
                .map(i->i*2)
        )
                .to().streamable();

        assertThat(repeat.stream().toList().size(),equalTo(Arrays.asList(2,4,6,8,10,12).size()));
        assertThat(repeat.stream().toList().size(),equalTo(Arrays.asList(2,4,6,8,10,12).size()));
    }

    @Test
    public void testScanRightSumMonoid() {
        assertThat(of("a", "ab", "abc").peek(System.out::println)
                .map(str -> str.length())
                .peek(System.out::println)
                .scanRight(Reducers.toTotalInt()).toList().size(), is(4));

    }
    @Test
    public void combineNoOrder(){
        assertThat(of(1,2,3)
                .combine((a, b)->a.equals(b), Semigroups.intSum)
                .to().listX().size(),equalTo(ListX.of(1,3,2).size()));

    }
    @Test
    public void testScanLeftStringConcatMonoid() {
        assertThat(of("a", "b", "c").scanLeft(Reducers.toString("")).toList().size(), is(asList("", "a", "ab", "abc").size()));
    }
    @Test
    public void testScanLeftSumMonoid() {

        assertThat(of("a", "ab", "abc").map(str -> str.length()).
                peek(System.out::println).scanLeft(Reducers.toTotalInt()).toList().size(), is(asList(0, 6, 3, 2).size()));
    }
    @Test
    public void onEmptySwitch() {
        assertThat(VavrHashSetX.empty()
                          .onEmptySwitch(() -> PersistentSetX.of(1, 2, 3)).toList(),
                   equalTo(VavrHashSetX.of(1, 2, 3).toList()));
    }
    @Test
    public void forEach2() {

        assertThat(of(1, 2, 3).forEach2(a -> Vector.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), (a, b) -> a + b).size(),
                equalTo(12));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.oath.cyclops.functions.collections.extensions.AbstractCollectionXTest#
     * empty()
     */
    @Override
    public <T> FluentCollectionX<T> empty() {
        return VavrHashSetX.empty();
    }



    @Test
    public void remove() {

        VavrHashSetX.of(1, 2, 3)
               .removeAll((Iterable<Integer>) BagX.of(2, 3))
               .mergeMap(i -> Flux.just(10 + i, 20 + i, 30 + i));

    }

    @Override
    public FluentCollectionX<Integer> range(int start, int end) {
        return VavrHashSetX.range(start, end);
    }

    @Override
    public FluentCollectionX<Long> rangeLong(long start, long end) {
        return VavrHashSetX.rangeLong(start, end);
    }

    @Override
    public <T> FluentCollectionX<T> iterate(int times, T seed, UnaryOperator<T> fn) {
        return VavrHashSetX.iterate(times, seed, fn);
    }

    @Override
    public <T> FluentCollectionX<T> generate(int times, Supplier<T> fn) {
        return VavrHashSetX.generate(times, fn);
    }

    @Override
    public <U, T> FluentCollectionX<T> unfold(U seed, Function<? super U, Option<Tuple2<T, U>>> unfolder) {
        return VavrHashSetX.unfold(seed, unfolder);
    }
    @Test
    public void takeWhileTest(){





    }
    @Test
    public void limitWhileTest(){






    }
}
