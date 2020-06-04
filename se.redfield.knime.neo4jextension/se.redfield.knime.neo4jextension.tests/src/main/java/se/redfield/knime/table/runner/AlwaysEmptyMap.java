/**
 *
 */
package se.redfield.knime.table.runner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class AlwaysEmptyMap<K, V> extends ConcurrentHashMap<K, V> {
    private static final long serialVersionUID = 1L;

    @Override
    public V put(final K key, final V value) {
        return value;
    }
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
    }
    @Override
    public V putIfAbsent(final K key, final V value) {
        return value;
    }
}
