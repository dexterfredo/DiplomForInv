package ru.inversion.LoaderMicexFX.gateway;

import com.micex.client.Binder;
import com.micex.client.Filler;
import com.micex.client.Meta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.inversion.LoaderMicexFX.model.MicexTableRow;
import ru.inversion.LoaderMicexFX.service.BufferConfigService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MicexMarketBinder implements Binder {

    private final Map<String, Table> database = new HashMap<>();
    private final List<MicexTableRow> micexTableRowsSink = new ArrayList<>();
    private final Map<String, Integer> emittedRowsByTable = new HashMap<>();
    private volatile boolean staticEmission;
    private final int maxRowsPerParse;
    private final BufferConfigService bufferConfigService;
    private final MicexTableSchemaRegistry schemaRegistry;

    public MicexMarketBinder(
            BufferConfigService bufferConfigService,
            MicexTableSchemaRegistry schemaRegistry,
            @Value("${app.parse.max-rows:2000}") int maxRowsPerParse) {
        this.bufferConfigService = bufferConfigService;
        this.schemaRegistry = schemaRegistry;
        this.maxRowsPerParse = maxRowsPerParse > 0 ? maxRowsPerParse : 2000;
    }

    public void reset() {
        synchronized (micexTableRowsSink) {
            micexTableRowsSink.clear();
        }
        emittedRowsByTable.clear();
        database.clear();
    }

    public void setStaticEmission(boolean staticEmission) {
        this.staticEmission = staticEmission;
    }

    public List<MicexTableRow> drainMicexTableRows() {
        synchronized (micexTableRowsSink) {
            List<MicexTableRow> copy = new ArrayList<>(micexTableRowsSink);
            micexTableRowsSink.clear();
            emittedRowsByTable.clear();
            return copy;
        }
    }

    @Override
    public Filler getFiller(Meta.Message source) {
        schemaRegistry.captureFromMessage(source);
        String name = source.name;
        Table table = database.get(name);
        if (table == null) {
            table = new Table();
            database.put(name, table);
        }
        return table;
    }

    private void emitRow(Meta.Message table, Record rec) {
        if (rec == null || rec.values.isEmpty()) {
            return;
        }
        String tableName = table.name == null ? "" : table.name.toUpperCase();
        if (!isBufferedMicexTable(tableName)) {
            return;
        }
        synchronized (micexTableRowsSink) {
            int tableRows = emittedRowsByTable.getOrDefault(tableName, 0);
            if (tableRows >= maxRowsPerParse) {
                return;
            }
            emittedRowsByTable.put(tableName, tableRows + 1);
            MicexTableRow row = new MicexTableRow();
            row.setTableName(tableName);
            row.setSnapshot(staticEmission);
            for (Map.Entry<String, Object> e : rec.values.entrySet()) {
                row.putField(e.getKey(), e.getValue());
            }
            micexTableRowsSink.add(row);
        }
    }

    private boolean isBufferedMicexTable(String tableName) {
        Set<String> tables = bufferConfigService.getMicexTableNames();
        if (tables.isEmpty()) {
            return Set.of("SECURITIES", "TRADES", "ORDERS", "BOARDS", "TESYSTIME").contains(tableName);
        }
        return tables.contains(tableName);
    }

    static class Record {
        int decimals;
        final Map<String, Object> values = new LinkedHashMap<>();
    }

    class Table implements Filler {

        final Map<String, Record> records = new HashMap<>();
        final Map<String, List<Record>> orderbooks = new HashMap<>();
        final Map<String, Object> keys = new LinkedHashMap<>();
        Record current;
        List<Record> orderbook;

        @Override
        public boolean initTableUpdate(Meta.Message table) {
            if (table.isClearOnUpdate()) {
                records.clear();
            }
            return true;
        }

        @Override
        public void doneTableUpdate(Meta.Message table) {
            orderbook = null;
        }

        @Override
        public void setKeyValue(Meta.Field field, Object value) {
            keys.put(field.name, MicexFieldConverter.asMicexString(value));
        }

        @Override
        public boolean initRecordUpdate(Meta.Message table) {
            if (table.isOrderbook()) {
                if (orderbook == null) {
                    orderbook = new ArrayList<>();
                }
                current = new Record();
                orderbook.add(current);
                return true;
            }
            if (keys.isEmpty()) {
                current = new Record();
                records.put(Integer.toString(records.size()), current);
                return true;
            }
            String key = keys.toString();
            current = records.get(key);
            if (current == null) {
                current = new Record();
                records.put(key, current);
            }
            return true;
        }

        @Override
        public void setRecordDecimals(int decimals) {
            current.decimals = decimals;
        }

        @Override
        public int getRecordDecimals() {
            return current.decimals;
        }

        @Override
        public void setFieldValue(Meta.Field field, Object value) {
            current.values.put(field.name, MicexFieldConverter.asMicexString(value));
        }

        @Override
        public void doneRecordUpdate(Meta.Message table) {
            emitRow(table, current);
            keys.clear();
            current = null;
        }

        @Override
        public void switchOrderbook(Meta.Message table, Meta.Ticker ticker) {
            orderbook = orderbooks.get(ticker.toString());
            if (orderbook == null) {
                orderbook = new ArrayList<>();
                orderbooks.put(ticker.toString(), orderbook);
            } else {
                orderbook.clear();
            }
        }
    }
}
