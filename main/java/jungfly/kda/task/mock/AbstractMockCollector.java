package jungfly.kda.task.mock;

import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractMockCollector implements Collector<byte[]> {
    private static final Logger log = LoggerFactory.getLogger(AbstractMockCollector.class);


    @Override
    public void close() {

    }
}
