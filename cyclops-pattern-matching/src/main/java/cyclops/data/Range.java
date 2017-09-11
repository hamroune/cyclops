package cyclops.data;


import com.sun.tools.corba.se.idl.constExpr.Or;
import cyclops.control.Maybe;
import cyclops.functions.Ordering;
import cyclops.stream.ReactiveSeq;
import cyclops.typeclasses.Enumeration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Comparator;

import static org.jooq.lambda.tuple.Tuple.tuple;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Range<T> {
    private final T start;
    private final T end;
    private final Enumeration<T> enm;
    private final Ordering<? super T> comp;

    public static <T> Range<T> range(T start, T end, Enumeration<T> enm, Comparator<? super T> comp){
        return new Range<>(start,end,enm, Ordering.of(comp));
    }
    public static <T extends Comparable<T>> Range<T> range(T start, T end, Enumeration<T> enm){
        return new Range<>(start,end,enm, Ordering.of(Comparator.naturalOrder()));
    }
    public Range<T> reverse(){
        return range(end,start,enm,comp);
    }
    public boolean contains(T value){
        if(comp.isGreaterThanOrEqual(value,start) && comp.isLessThanOrEqual(value,end))
            return true;
        return false;
    }
    public boolean contains(Range<T> compareTo){
        return comp.isLessThanOrEqual(start,compareTo.start) && comp.isGreaterThanOrEqual(end,compareTo.end);
    }


    public Tuple2<Range<T>,Maybe<Range<T>>> plusAll(Range<T> range){
        //1 .. x    >=1 .. y
        if(comp.isLessThanOrEqual(start,range.start)){
            if(range.contains(end)){
                return tuple(range(start,range.end,enm,comp),Maybe.none());
            }
            if(end.equals(enm.succ(range.end).get())){
                return tuple(range(start,end,enm,comp),Maybe.none());
            }
            return tuple(this,Maybe.just(range));

        }else{
            if(this.contains(range.end)){
                return tuple(range(range.start,end,enm,comp),Maybe.none());
            }if(range.end.equals(enm.succ(end).get())){
                return tuple(range(start,range.end,enm,comp),Maybe.none());
            }
            else{
                return tuple(range,Maybe.just(this));
            }
        }

    }
    public Maybe<Tuple2<Range<T>,Maybe<Range<T>>>> minusAll(Range<T> range){
        //            |         |  <--range
        // |    |
        if (comp.isLessThan(end, range.start)) {
            return Maybe.just(tuple(this, Maybe.none()));
        }
        //                           |   |
        if(comp.isGreaterThanOrEqual(start,range.end)){
            return Maybe.just(tuple(this, Maybe.none()));
        }
        //                 | |
        if(range.contains(this)){
            return Maybe.none();
        }
        if(comp.isLessThanOrEqual(start,range.start)){
            if(comp.isLessThanOrEqual(end,range.end))
                return Maybe.just(tuple(range(start,range.start,enm,comp),Maybe.none()));
            else
                return Maybe.just(tuple(range(start,range.start,enm,comp),Maybe.just(range(range.end,end,enm,comp))));
        }

        //     |              |  <--range
        // |       |
        // |                            |
        //               |            |
        return Maybe.just(tuple(range(range.end,end,enm,comp),Maybe.none()));

    }


    public Maybe<Range<T>> intersection(Range<T> toMerge) {

        T newStart = (T) comp.max(this.start, toMerge.start);
        T newEnd = (T) comp.min(this.end, toMerge.end);
        if (comp.isLessThanOrEqual(start, end))
            return Maybe.just(range(start, end, enm, comp));
        return Maybe.none();
    }

    public ReactiveSeq<T> stream(){
        return ReactiveSeq.iterate(start,e->!end.equals(e),e->enm.succ(e).get());
    }

    public LazySeq<T> lazySeq(){
        int order = comp.compare(start,end);
        if(order==0){
            return LazySeq.of(start);
        }if(order<0){
            return LazySeq.cons(start,()->range(enm.succ(start).get(),end,enm,comp).lazySeq());
        }
        return LazySeq.cons(start,()->range(enm.pred(start).get(),end,enm,comp).lazySeq());
    }


}
