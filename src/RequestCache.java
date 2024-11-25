import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

public class RequestCache {
    private final Map<String, String> cache;
    private final int capacity;
    private final BloomFilter<String> bloomFilter;

    /**
     * Конструктор кэша запросов с фильтром Блума.
     *
     * @param capacity         Максимальный размер кэша.
     * @param bloomFilterSize  Размер фильтра Блума.
     * @param errorRate        Допустимая вероятность ложного срабатывания.
     */
    public RequestCache(int capacity, int bloomFilterSize, double errorRate) {
        this.capacity = capacity;//хранит макс размер кэша, ес кол-во элементов привышает, то старейший замен новейшим
        this.cache = new LinkedHashMap<String, String>(capacity, 0.75f, true) {
            //LinkedHashMap упорядычивает элементы в порядке их использования
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                //removeEldestEntry ответственный за удаление старейшего элемента))
                return size() > RequestCache.this.capacity;
            }
        };
        this.bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), bloomFilterSize, errorRate);
        //Funnels.stringFunnel преобразует строковые данные в байты
        //bloomFilterSize макс кол-во элементов, который фильтр должен уметь обрабатывать
        //errorRate вероятность ложного срабатывания
    }

    public String getRequest(String key, FetchFunction fetchFunction) {
        // Проверка в кэше
        if (cache.containsKey(key)) {
            System.out.println("Кэш найден для ключа: " + key);
            return cache.get(key);
        }

        // Проверка в фильтре Блума( ес отсутсвует в кэш)
        if (bloomFilter.mightContain(key)) {
            System.out.println("Ключ " + key + " найден в фильтре Блума. Кэшируем результат.");
            String result = fetchFunction.fetch(key);
            cache.put(key, result);
            return result;
        } else {
            System.out.println("Ключ " + key + " не найден в фильтре Блума. Добавляем, но не кэшируем.");
            bloomFilter.put(key);
            return fetchFunction.fetch(key);
        }
    }

    @FunctionalInterface
    public interface FetchFunction {
        String fetch(String key);
    }

    public static void main(String[] args) throws InterruptedException {
        RequestCache cache = new RequestCache(5, 10000, 0.01);

        // Пример запросов
        String[] keys = {"A", "B", "A", "C", "A", "D", "E", "F", "A", "B", "G", "A"};

        for (String key : keys) {
            String result = cache.getRequest(key, k -> {
                try {
                    System.out.println("Запрос данных для ключа: " + k);
                    Thread.sleep(1000); // Имитация задержки
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "Данные для " + k;
            });
            System.out.println("Результат для ключа " + key + ": " + result + "\n");
        }
    }
}

