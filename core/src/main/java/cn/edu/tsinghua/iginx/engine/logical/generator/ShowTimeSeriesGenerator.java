package cn.edu.tsinghua.iginx.engine.logical.generator;

import cn.edu.tsinghua.iginx.engine.shared.operator.Operator;
import cn.edu.tsinghua.iginx.engine.shared.operator.ShowTimeSeries;
import cn.edu.tsinghua.iginx.engine.shared.source.GlobalSource;
import cn.edu.tsinghua.iginx.sql.statement.Statement;
import cn.edu.tsinghua.iginx.sql.statement.StatementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShowTimeSeriesGenerator implements LogicalGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ShowTimeSeriesGenerator.class);
    private final static ShowTimeSeriesGenerator instance = new ShowTimeSeriesGenerator();
    private final GeneratorType type = GeneratorType.ShowTimeSeries;

    private ShowTimeSeriesGenerator() {
    }

    public static ShowTimeSeriesGenerator getInstance() {
        return instance;
    }

    @Override
    public GeneratorType getType() {
        return type;
    }

    @Override
    public Operator generate(Statement statement) {
        if (statement == null)
            return null;
        if (statement.getType() != StatementType.SHOW_TIME_SERIES)
            return null;
        return new ShowTimeSeries(new GlobalSource());
    }
}
