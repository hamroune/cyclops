package com.aol.cyclops.rx2.adapter;

import com.aol.cyclops2.types.extensability.AbstractFunctionalAdapter;
import cyclops.async.Future;

import cyclops.monads.AnyM;
import cyclops.monads.Rx2Witness;
import cyclops.monads.Rx2Witness.single;
import cyclops.stream.ReactiveSeq;
import io.reactivex.Single;
import lombok.AllArgsConstructor;


import java.util.function.Function;
import java.util.function.Predicate;




@AllArgsConstructor
public class SingleAdapter extends AbstractFunctionalAdapter<single> {



    @Override
    public <T> Iterable<T> toIterable(AnyM<single, T> t) {
        return Future.fromPublisher(future(t));
    }

    @Override
    public <T, R> AnyM<single, R> ap(AnyM<single,? extends Function<? super T,? extends R>> fn, AnyM<single, T> apply) {
        Single<T> f = future(apply);
        Single<? extends Function<? super T, ? extends R>> fnF = future(fn);
        Single<R> res = Single.fromFuture(fnF.toFuture().thenCombine(f.toFuture(), (a, b) -> a.apply(b)));
        return Singles.anyM(res);

    }

    @Override
    public <T> AnyM<single, T> filter(AnyM<single, T> t, Predicate<? super T> fn) {
        return Singles.anyM(future(t).filter(fn));
    }

    <T> Single<T> future(AnyM<single,T> anyM){
        return anyM.unwrap();
    }
    <T> Future<T> futureW(AnyM<single,T> anyM){
        return Future.fromPublisher(anyM.unwrap());
    }

    @Override
    public <T> AnyM<single, T> empty() {
        return Singles.anyM(Single.empty());
    }



    @Override
    public <T, R> AnyM<single, R> flatMap(AnyM<single, T> t,
                                     Function<? super T, ? extends AnyM<single, ? extends R>> fn) {
        return Singles.anyM(Single.from(futureW(t).flatMap(fn.andThen(a-> futureW(a)))));

    }

    @Override
    public <T> AnyM<single, T> unitIterable(Iterable<T> it)  {
        return Singles.anyM(Single.from(Future.fromIterable(it)));
    }

    @Override
    public <T> AnyM<single, T> unit(T o) {
        return Singles.anyM(Single.just(o));
    }

    @Override
    public <T> ReactiveSeq<T> toStream(AnyM<single, T> t) {
        return ReactiveSeq.fromPublisher(single(t));
    }

}
