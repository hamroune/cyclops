package com.aol.cyclops.matcher;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import lombok.val;

import org.hamcrest.Matcher;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import com.aol.cyclops.matcher.PatternMatcher.Action;
import com.aol.cyclops.matcher.PatternMatcher.ActionWithReturn;
import com.aol.cyclops.matcher.PatternMatcher.ActionWithReturnWrapper;
import com.aol.cyclops.matcher.PatternMatcher.Extractor;

public class Matching {
	
	public static <R,V,T,X> TypeSafePatternMatcher<T, X> inCaseOfIterable(
			Iterable<Predicate<V>> predicates, ActionWithReturn<List<V>, X> a) {

		return new TypeSafePatternMatcher<T,X>().inCaseOfIterable(predicates, a);
	}

	public static <R,V,T,X> TypeSafePatternMatcher<T, X> inMatchOfIterable(
			Iterable<Matcher> predicates, ActionWithReturn<List<V>, X> a) {
		return new TypeSafePatternMatcher<T,X>().inMatchOfIterable(predicates, a);
	}

	public static <R,V,V1,T,X> TypeSafePatternMatcher<T, X> inMatchOfMatchers(
			Tuple2<Matcher<V>, Matcher<V1>> predicates,
			ActionWithReturn<R, X> a, Extractor<T, R> extractor) {

		return new TypeSafePatternMatcher<T,X>().inMatchOfMatchers(predicates, a, extractor);
	}

	public static <R,V,V1,T,X> TypeSafePatternMatcher<T, X> inCaseOfPredicates(
			Tuple2<Predicate<V>, Predicate<V1>> predicates,
			ActionWithReturn<R, X> a, Extractor<T, R> extractor) {

		return new TypeSafePatternMatcher<T,X>().inCaseOfPredicates(predicates, a, extractor);
	}

	public static <R,V,T,X> TypeSafePatternMatcher<T, X> inCaseOfTuple(Tuple predicates,
			ActionWithReturn<R, X> a, Extractor<T, R> extractor) {

		return new TypeSafePatternMatcher<T,X>().inCaseOfTuple(predicates, a, extractor);
	}

	public static <R,V,T,X> TypeSafePatternMatcher<T, X> inMatchOfTuple(Tuple predicates,
			ActionWithReturn<R, X> a, Extractor<T, R> extractor) {
		return new TypeSafePatternMatcher<T,X>().inMatchOfTuple(predicates, a, extractor);
	}
	
	public static <R,V,T,X> TypeSafePatternMatcher<T, X> caseOfIterable(Iterable<Predicate<V>> predicates,Action<List<V>> a){
		return new TypeSafePatternMatcher<T,X>().caseOfIterable(predicates, a);
		
	}
	
	public static <R,V,T,X> TypeSafePatternMatcher<T, X> matchOfIterable(Iterable<Matcher> predicates,Action<List<V>> a){
		return new TypeSafePatternMatcher<T,X>().matchOfIterable(predicates, a);
		
	}
	
	public static <R,V,V1,T,X>  TypeSafePatternMatcher<T, X> matchOfMatchers(Tuple2<Matcher<V>,Matcher<V1>> predicates,
			Action<R> a,Extractor<T,R> extractor){
		return new TypeSafePatternMatcher<T,X>().matchOfMatchers(predicates, a, extractor);
		
	}
	
	public static <R,V,V1,T,X> TypeSafePatternMatcher<T, X> caseOfPredicates(Tuple2<Predicate<V>,Predicate<V1>> predicates,
			Action<R> a,Extractor<T,R> extractor){
		return new TypeSafePatternMatcher<T,X>().caseOfPredicates(predicates, a, extractor);
		
	}
			
	public static <R,V,T,X> TypeSafePatternMatcher<T, X> caseOfTuple(Tuple predicates, Action<R> a,Extractor<T,R> extractor){
		return new TypeSafePatternMatcher<T,X>().caseOfTuple(predicates, a, extractor);
		
	}
			
	public static <R,V,T,X> TypeSafePatternMatcher<T, X> matchOfTuple(Tuple predicates, Action<R> a,Extractor<T,R> extractor){
		return new TypeSafePatternMatcher<T,X>().matchOfTuple(predicates, a, extractor);
	}

	public static <R,V,T,X> TypeSafePatternMatcher<T,X> caseOfType( Extractor<T,R> extractor,Action<V> a){
		return new TypeSafePatternMatcher<T,X>().caseOfType(extractor,a);
		
	}
	public static <R,V,T,X> TypeSafePatternMatcher<T,X> caseOfValue(R value, Extractor<T,R> extractor,Action<V> a){
		
		return new TypeSafePatternMatcher<T,X>().caseOfValue(value,extractor,a);
	}
	
	
	public static <V,T,X> TypeSafePatternMatcher<T,X> caseOfValue(V value,Action<V> a){
		
		return new TypeSafePatternMatcher<T,X>().caseOfValue(value,a);
	}
	public static <V,T,X> TypeSafePatternMatcher<T,X> caseOfType(Action<V> a){
		return new TypeSafePatternMatcher<T,X>().<V>caseOfType(a);
		
	}

	public static <V,T,X> TypeSafePatternMatcher<T,X> caseOf(Predicate<V> match,Action<V> a){
		return new TypeSafePatternMatcher<T,X>().caseOf(match, a);
	}
	public static <R,V,T,X> TypeSafePatternMatcher<T,X> caseOfThenExtract(Predicate<V> match,Action<R> a, Extractor<T,R> extractor){
		
		return new TypeSafePatternMatcher<T,X>().caseOfThenExtract(match,a,extractor);
	}
	
	
	public  static <R,V,T,X> TypeSafePatternMatcher<T,X> caseOf( Extractor<T,R> extractor,Predicate<R> match,Action<V> a){
		
		return new TypeSafePatternMatcher<T,X>().caseOf(extractor, match, a);
	}
	
	public static <V,T,X> TypeSafePatternMatcher<T,X> inCaseOfValue(V value,ActionWithReturn<V,X> a){
		return new TypeSafePatternMatcher<T,X>().inCaseOfValue(value, a);
	}
	public static <V,T,X> TypeSafePatternMatcher<T,X> inCaseOfType(ActionWithReturn<T,X> a){
		return new TypeSafePatternMatcher<T,X>().inCaseOfType(a);
		
	}
	public static <V,T,X> TypeSafePatternMatcher<T,X> inCaseOf(Predicate<V> match,ActionWithReturn<V,X> a){
		return new TypeSafePatternMatcher<T,X>().inCaseOf(match, a);
	}
	public static <R,V,T,X> TypeSafePatternMatcher<T,X> inCaseOfThenExtract(Predicate<T> match,ActionWithReturn<R,X> a, Extractor<T,R> extractor){
		return new TypeSafePatternMatcher<T,X>().inCaseOfThenExtract(match, a, extractor);
	}
	
	
	public static <R,V,T,X> TypeSafePatternMatcher<T,X> inCaseOf( Extractor<T,R> extractor,Predicate<V> match,ActionWithReturn<V,X> a){
		return new TypeSafePatternMatcher<T,X>().inCaseOf(extractor, match, a);
	}
	

	
	public static <R,V,T,X> TypeSafePatternMatcher<T,X> inCaseOfType( Extractor<T,R> extractor,ActionWithReturn<V,X> a){
		return new TypeSafePatternMatcher<T,X>().inCaseOfType(extractor, a);
	}
	public static <R,V,T,X> TypeSafePatternMatcher<T,X> inCaseOfValue(R value, Extractor<T,R> extractor,ActionWithReturn<V,X> a){
		return new TypeSafePatternMatcher<T,X>().inCaseOfValue(value, extractor, a);
	}
	
	
	public  static <R,T,X> TypeSafePatternMatcher<T,X>  matchOf( Extractor<T,R> extractor,Matcher<R> match,Action<R> a){
		return new TypeSafePatternMatcher<T,X>().matchOf(extractor, match, a);
	}
	
	public static <V,T,X> TypeSafePatternMatcher<T,X> matchOf(Matcher<V> match,Action<V> a){
		return new TypeSafePatternMatcher<T,X>().matchOf(match,a);
	}
	
	public  static <R,V,T,X> TypeSafePatternMatcher<T,X> matchOfThenExtract(Matcher<V> match,Action<R> a, Extractor<T,R> extractor){
		return new TypeSafePatternMatcher<T,X>().matchOfThenExtract((Matcher)match, a, extractor);
	}
	
	
	
	public static <V,T,X> TypeSafePatternMatcher<T,X> inMatchOf(Matcher<V> match,ActionWithReturn<V,X> a){
		return new TypeSafePatternMatcher<T,X>().inMatchOf(match, a);
	}
	
	public static <R,T,X> TypeSafePatternMatcher<T,X> inMatchOfThenExtract(Matcher<T> match,ActionWithReturn<R,X> a, Extractor<T,R> extractor){
		return new TypeSafePatternMatcher<T,X>().inMatchOfThenExtract(match, a, extractor);
	}
	
	
	public static  <R,V,T,X> TypeSafePatternMatcher<T,X> inMatchOf( Extractor<T,R> extractor,Matcher<V> match,ActionWithReturn<V,X> a){
		return new TypeSafePatternMatcher<T,X>().inMatchOf(extractor, match, a);
	}
	
}