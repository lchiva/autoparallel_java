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

public class ParallelTest {
	static int x=0;

	public static void main() {
		List<Integer> nombres=new ArrayList<>();
		List<Integer> nombres2=new ArrayList<>();

		int sum=0;

		boolean b = true;


		//not parallelisable, modify field
		sum+=nombres.stream().mapToInt(i->ParallelTest.x+=1).sum();

		//parallelisable
		sum+=nombres.stream().mapToInt(i->i).sum();
		sum-=nombres.stream().filter(i->i>0).mapToInt(i->i).sum();
		b &= nombres.stream().allMatch(i->i>0);
		b |= nombres.stream().filter(i->i<15).anyMatch(i->i>0);

		//parallelisable
		nombres2.addAll(nombres.stream().filter(i->i<15).map(i->i-15).collect(Collectors.toList()));
		//parallelisable
		nombres.stream().forEach(i->i+=1);
		//not parallelisable
		nombres.stream().forEach(i->nombres2.add(i));
		//not parallelisable, calls .add()
		nombres2.addAll(nombres.stream().filter(i->i<15).map(i->{nombres2.add(i);return i;}).collect(Collectors.toList()));

		//not parallelisable, calls .add()
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

		//parallelisable : ordered, without SIOs
		Arrays.stream(new int[] { 1, 2, 3 }).map(s-> s*2+1).average().ifPresent(System.out::println); // 5.0
		//parallelisable
		Collection<Integer> pq = new PriorityQueue<>();
		pq.add(5);
		pq.add(8);
		pq.add(3);
		pq.stream().map(s -> s + 1).sorted().collect(Collectors.toCollection(TreeSet::new));
		//parallelisable
		int sum1=0;
		List<Integer> nombres1=new ArrayList<>();
		sum1+=nombres1.stream().mapToInt(i->i).sum();

		Stream.generate(new Random()::nextInt)
	    .limit(5).forEach(System.out::println);
	}
}


