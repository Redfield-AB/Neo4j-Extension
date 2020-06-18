/**
 *
 */
package se.redfield.knime.neo4j.async;

import java.util.function.Consumer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConsumerProxy<T> implements Consumer<T> {
    private Consumer<T> consumer;
    public ConsumerProxy() {
        super();
    }
    @Override
    public void accept(final T t) {
        consumer.accept(t);
    }
    public void setConsumer(final Consumer<T> consumer) {
        this.consumer = consumer;
    }
}
