package cyclops.collections.adt;


import cyclops.patterns.CaseClass2;
import cyclops.patterns.Sealed4;
import cyclops.stream.ReactiveSeq;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;
import org.agrona.collections.ArrayUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.copyOf;


@AllArgsConstructor
public class HAMT<K, V>  {
    static final int BITS_IN_INDEX = 5;
    static final int SIZE = (int) StrictMath.pow(2, BITS_IN_INDEX);
    static final int MIN_INDEX = 0;
    static final int MAX_INDEX = SIZE - 1;
    static final int MASK = (1 << BITS_IN_INDEX) - 1;

    public static <K,V> Node<K,V> empty(){
        return EmptyNode.Instance;
    }

   interface Node<K,V>{

       public Node<K,V> plus(int bitShiftDepth,int hash,K key, V value);
       public Optional<V> get(int bitShiftDepth,int hash,K key);
       public Node<K,V> minus(int bitShiftDepth,int hash,K key);
       int size();
       LazyList<Tuple2<K,V>> lazyList();
       ReactiveSeq<Tuple2<K, V>> stream();
   }

   public static class EmptyNode<K,V> implements Node<K,V>{
     static final EmptyNode Instance = new EmptyNode();
       @Override
       public Node<K, V> plus(int bitShiftDepth, int hash, K key, V value) {
           return new ValueNode<>(hash,key,value);
       }

       @Override
       public Optional<V> get(int bitShiftDepth, int hash, K key) {
           return Optional.empty();
       }

       @Override
       public Node<K, V> minus(int bitShiftDepth,int hash, K key) {
           return this;
       }

       @Override
       public int size() {
           return 0;
       }

       @Override
       public LazyList<Tuple2<K, V>> lazyList() {
           return LazyList.empty();
       }

       @Override
       public ReactiveSeq<Tuple2<K, V>> stream() {
           return ReactiveSeq.empty();
       }
   }
   @AllArgsConstructor
   public static class ValueNode<K,V> implements Node<K,V>, CaseClass2<K,V>{

       private final int hash;
       public final K key;
       public final V value;
       @Override
       public Node<K, V> plus(int bitShiftDepth, int hash, K key, V value) {
           ValueNode<K,V> newNode = new ValueNode<>(hash,key,value);
           return isMatch(hash, key) ? newNode : merge(bitShiftDepth,newNode);
       }

       private Node<K,V> merge(int bitShiftDepth,  ValueNode<K, V> that) {
           //hash each merge into a collision node if hashes are the same, otherwise store in new location under a BitsetNode
           if(hash==that.hash)
                return new CollisionNode<>(hash,LazyList.of(Tuple.tuple(key,value),that.unapply()));
           //create new BitsetNode
           int posThis = BitsetNode.bitpos(hash,bitShiftDepth);
           int posThat = BitsetNode.bitpos(that.hash,bitShiftDepth);
           int newBitset = posThis | posThat;
           if(posThis==posThat) { //collision
              Node<K,V> merged = merge(bitShiftDepth+BITS_IN_INDEX,that);
              return new BitsetNode<>(newBitset,2,new Node[]{merged});
           }
           Node<K,V>[] ordered = posThis<posThat ? new Node[]{this,that} : new Node[]{that,this};
           return new BitsetNode<>(newBitset,2,ordered);
       }


       @Override
       public Optional<V> get(int bitShiftDepth, int hash, K key) {
           return isMatch(hash, key) ? Optional.ofNullable(value) : Optional.empty();
       }

       private boolean isMatch(int hash, K key) {
           return this.hash==hash && Objects.equals(this.key,key);
       }

       @Override
       public Node<K, V> minus(int bitShiftDepth,int hash,K key) {
           return isMatch(hash, key) ? EmptyNode.Instance : this;
       }
       public int hash(){
           return hash;
       }

       @Override
       public int size() {
           return 1;
       }

       @Override
       public LazyList<Tuple2<K, V>> lazyList() {
           return LazyList.of(unapply());
       }

       @Override
       public ReactiveSeq<Tuple2<K, V>> stream() {
           return ReactiveSeq.empty();
       }

       @Override
       public Tuple2<K, V> unapply() {
           return Tuple.tuple(key,value);
       }
   }
    @AllArgsConstructor
   public static class CollisionNode<K,V> implements Node<K,V>{

       private final int hash;
       private final LazyList<Tuple2<K,V>> bucket;
       @Override
       public Node<K, V> plus(int bitShiftDepth, int hash, K key, V value) {
           LazyList<Tuple2<K, V>> filtered = bucket.filter(t -> !Objects.equals(key, t.v1));
           Node<K,V> newNode = filtered.size()==0 ?  new ValueNode<>(hash,key,value) : new CollisionNode<>(hash,filtered.prepend(Tuple.tuple(key,value)));
           if(this.hash==hash){
               return newNode;
           }
           return merge(bitShiftDepth,hash,newNode);
       }

        private Node<K,V> merge(int bitShiftDepth, int thatHash,Node<K, V> that) {
            //hash each merge into a collision node if hashes are the same, otherwise store in new location under a BitsetNode
            if(hash==thatHash)
                return new CollisionNode<>(hash,bucket.prependAll(that.lazyList()));
            //create new BitsetNode
            int posThis = BitsetNode.bitpos(hash,bitShiftDepth);
            int posThat = BitsetNode.bitpos(thatHash,bitShiftDepth);
            int newBitset = posThis | posThat;
            if(posThis==posThat) { //collision
                Node<K,V> merged = merge(bitShiftDepth+BITS_IN_INDEX,thatHash,that);
                return new BitsetNode<>(newBitset,2,new Node[]{merged});
            }
            Node<K,V>[] ordered = posThis>posThat ? new Node[]{this,that} : new Node[]{that,this};
            return new BitsetNode<>(newBitset,2,ordered);
        }

        @Override
       public Optional<V> get(int bitShiftDepth, int hash, K key) {
           if(this.hash==hash){
               return bucket.stream().filter(t->Objects.equals(key,t.v1)).findFirst().map(Tuple2::v2);
           }
           return Optional.empty();
       }

       @Override
       public Node<K, V> minus(int bitShiftDepth,int hash, K key) {
           if(this.hash==hash){
               return new CollisionNode<>(hash,bucket.filter(t->!Objects.equals(key,t.v1)));
           }
           return this;
       }

        @Override
        public int size() {
            return bucket.size();
        }

        @Override
        public LazyList<Tuple2<K, V>> lazyList() {
            return bucket;
        }

        @Override
        public ReactiveSeq<Tuple2<K, V>> stream() {
            return bucket.stream();
        }
    }
    @AllArgsConstructor
   public static class BitsetNode<K,V> implements Node<K,V>{
       private final int bitset;
       private final int size;
       private final Node<K,V>[] nodes;

        @Override
        public Node<K, V> plus(int bitShiftDepth, int hash, K key, V value) {
            int bitPos = bitpos(hash, bitShiftDepth);
            int arrayPos = index(bitPos);
            Node<K,V> node = (absent(bitPos) ? EmptyNode.Instance : nodes[arrayPos]).plus(bitShiftDepth +BITS_IN_INDEX,hash,key,value);
            if(absent(bitPos)) {
                int addedBit = bitset | bitPos;
                Node<K, V>[] addedNodes = new Node[nodes.length + 1];
                System.arraycopy(nodes, 0, addedNodes, 0, arrayPos);
                addedNodes[arrayPos] = node;
                System.arraycopy(nodes, arrayPos, addedNodes, arrayPos + 1, nodes.length - arrayPos);
                return new BitsetNode<>(addedBit, size + node.size(), addedNodes);
            }else{
                Node<K,V>[] updatedNodes = Arrays.copyOf(nodes, nodes.length);
                updatedNodes[arrayPos] = node;
                return new BitsetNode<>(bitset, size + nodes[arrayPos].size(), updatedNodes);
            }

        }

        @Override
        public Optional<V> get(int bitShiftDepth, int hash, K key) {
            int pos = bitpos(hash, bitShiftDepth);
            return absent(pos)? Optional.empty() : find(bitShiftDepth,pos,hash,key);
        }

        public boolean absent(int pos){
            return (bitset & pos)==0;
        }

        private Optional<V> find(int shift,int pos, int hash, K key) {
            return nodes[index(pos)].get(shift+BITS_IN_INDEX,hash,key);
        }
        private Node<K,V> findNode(int pos) {
            return nodes[index(pos)];
        }

        @Override
        public Node<K, V> minus(int bitShiftDepth, int hash, K key) {
            int bitPos = bitpos(hash, bitShiftDepth);
            int arrayPos = index(bitPos);
            Node<K,V> node = (absent(bitPos) ? EmptyNode.Instance : nodes[arrayPos]).minus(bitShiftDepth +BITS_IN_INDEX,hash,key);
            int removedBit =   bitset & ~bitPos;
            Node<K,V>[] removedNodes = new Node[nodes.length - 1];
            System.arraycopy(nodes, 0, removedNodes, 0, arrayPos);
            System.arraycopy(nodes, arrayPos + 1, removedNodes, arrayPos, nodes.length - arrayPos - 1);
            return new BitsetNode<>(removedBit, size + node.size(), removedNodes);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public LazyList<Tuple2<K, V>> lazyList() {
            return LazyList.fromStream(stream());
        }
        @Override
        public ReactiveSeq<Tuple2<K, V>> stream() {
            return ReactiveSeq.of(nodes).flatMap(n -> n.stream());
        }
        static int bitpos(int hash, int shift){
            return 1 << mask(hash, shift);
        }
        static int mask(int hash, int shift){
            return (hash >>> shift) & (SIZE-1);
        }
        int index(int bit){
            return Integer.bitCount(bitset & (bit - 1));
        }
    }

}

