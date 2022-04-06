import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// sert à tester la parallélisation des streams

public class StreamExample {

    static int x = 0;
	public static void main(String[] args) {

		// not parallelisable: forEach create side effect and sorts is SIOs
		Collection<String> results = new ArrayList<>();
		Collection<String> set = new HashSet<>() {
		};
		set.stream()
			.filter(s -> s.startsWith("c")).map(String::toUpperCase).sorted().forEach(s -> results.add(s));

		//parallelisable : ordered without SIOs
		Arrays.asList("a1", "a2", "a3").stream().filter(s -> s.contains("a"))
				.collect(Collectors.toCollection(TreeSet::new));
		//not parallelisable : ordered, with SIOs (sort)
		Stream.of(1, 2, 3).sorted().findFirst().ifPresent(System.out::println); // a1
//
//		//parallelisable : ordered, without SIOs
//		List<Integer> l = new ArrayList<>();
		Arrays.stream(new int[] { 1, 2, 3 }).map(s-> s*2+1).average().ifPresent(System.out::println); // 5.0

		//parallelisable
		Collection<Integer> pq = new PriorityQueue<>();
		pq.add(5);
		pq.add(8);
		pq.add(3);
		pq.stream().map(s -> s + 1).sorted().collect(Collectors.toCollection(TreeSet::new));

		//parallelisable
		int sum=0;
		List<Integer> nombres=new ArrayList<>();
		sum+=nombres.stream().mapToInt(i->i).sum();
		
		Stream.generate(new Random()::nextInt)
	    .limit(5).forEach(System.out::println); 

        List<Integer> nombres=new ArrayList<Integer>();
		List<Integer> nombres2=new ArrayList<Integer>();

		int sum=0;
		
		boolean b = true;

		// Tout ceci doit marcher
		
		sum+=nombres.stream().mapToInt(i->StreamExample.x+=1).sum();
		
		sum+=nombres.stream().mapToInt(i->i).sum();
		sum-=nombres.stream().filter(i->i>0).mapToInt(i->i).sum();
		b &= nombres.stream().allMatch(i->i>0);
		b |= nombres.stream().filter(i->i<15).anyMatch(i->i>0);
		nombres2.addAll(nombres.stream().filter(i->i<15).map(i->i-15).collect(Collectors.toList()));		
		// Ca oui si les éléments distincts
		nombres.stream().forEach(i->i+=1);
		// Ca non
		nombres.stream().forEach(i->nombres2.add(i));
		nombres2.addAll(nombres.stream().filter(i->i<15).map(i->{nombres2.add(i);return i;}).collect(Collectors.toList()));

	    }
	}

