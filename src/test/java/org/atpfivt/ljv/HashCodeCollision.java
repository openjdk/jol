package org.atpfivt.ljv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michael Stummvoll
 * @author Ilya Selivanov
 */
public class HashCodeCollision {

    public List<String> genCollisionString(Integer len) {
        String str = "ab";
        str += str.toUpperCase();
        return genCollisionString(len, str);
    }

    public List<String> genCollisionString(Integer len, String alphabet) {
        Map<Integer, List<String>> hashMap = new HashMap<>();
        List<String> alphabet_list = alphabet.codePoints().mapToObj(c -> String.valueOf((char) c)).collect(Collectors.toUnmodifiableList());

        Stream<String> permuatation = alphabet_list.stream();

        for (int i = 0; i < len - 1; i++) {
            permuatation = permuatation
                    .flatMap(permuatatin_el -> alphabet_list.stream()
                            .map(alphabet_x -> permuatatin_el + alphabet_x));
        }

        permuatation.forEach(permuatation_el -> {
            Integer hash = permuatation_el.hashCode();
            if (!hashMap.containsKey(hash)) {
                hashMap.put(hash, new ArrayList<>());
            }
            hashMap.get(hash).add(permuatation_el);
        });


        return hashMap.values().stream().max(Comparator.comparingInt(List::size)).orElse(new ArrayList<>());
    }
}
