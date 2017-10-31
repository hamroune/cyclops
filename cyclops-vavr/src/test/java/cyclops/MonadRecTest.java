package cyclops;

import com.aol.cyclops.vavr.hkt.ListKind;
import com.oath.cyclops.hkt.Higher;
import cyclops.async.Future;

import cyclops.companion.vavr.Lists;
import cyclops.companion.vavr.Streams;
import cyclops.control.Either;
import cyclops.monads.VavrWitness;
import cyclops.monads.VavrWitness.list;
import cyclops.monads.VavrWitness.stream;
import cyclops.reactive.ReactiveSeq;
import cyclops.typeclasses.monad.MonadRec;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


public class MonadRecTest {

    @Test
    public void listTest(){
        MonadRec<list> mr = Lists.Instances.monadRec();
        List<Integer> l = Lists.tailRecEither(0, i -> i < 100_000 ? List.of(Either.left(i + 1)) : List.of(Either.right(i + 1)));
        assertThat(l,equalTo(List.of(100_001)));
    }
    @Test
    public void streamTest(){
        MonadRec<stream> mr = Streams.Instances.monadRec();
        Stream<Integer> l = Streams.tailRecEither(0, i -> i < 100_000 ? Stream.of(Either.left(i + 1)) : Stream.of(Either.right(i + 1)));
        assertThat(l,equalTo(Stream.of(100_001)));
    }
}
