package org.fhi360.lamis.modules.database.util;

import lombok.extern.slf4j.Slf4j;
import org.jumpmind.db.model.Table;
import org.jumpmind.extension.IExtensionPoint;
import org.jumpmind.symmetric.io.data.CsvData;
import org.jumpmind.symmetric.io.data.DataContext;
import org.jumpmind.symmetric.io.data.DataEventType;
import org.jumpmind.symmetric.io.data.writer.DatabaseWriterFilterAdapter;

@Slf4j
public class PkFilter extends DatabaseWriterFilterAdapter implements IExtensionPoint {

    @Override
    public boolean beforeWrite(DataContext context, Table table, CsvData data) {
        if ( data.getDataEventType().equals(DataEventType.INSERT) || data.getDataEventType().equals(DataEventType.UPDATE)) {
            String[] parsedData = data.getParsedData(CsvData.ROW_DATA);
            try {
                Object id = parsedData[table.getColumnIndex("id")];
                if (id == null) {
                    // remove the id column
                    data.removeParsedData("id");
                }
            }catch (Exception ignored){}

        }
        return true;
    }
}
