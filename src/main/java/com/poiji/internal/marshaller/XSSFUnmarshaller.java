package com.poiji.internal.marshaller;

import com.poiji.exception.PoijiException;
import com.poiji.internal.PoijiFile;
import com.poiji.internal.PoijiHandler;
import com.poiji.option.PoijiOptions;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.SAXHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by hakan on 22/10/2017
 */
final class XSSFUnmarshaller extends Deserializer {

    private final PoijiFile poijiFile;
    private final PoijiOptions options;
    private PoijiHandler poijiHandler;

    XSSFUnmarshaller(PoijiFile poijiFile, PoijiOptions options) {
        this.poijiFile = poijiFile;
        this.options = options;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> deserialize(Class<T> type) {
        try (OPCPackage open = OPCPackage.open(poijiFile.file())) {
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(open);
            XSSFReader reader = new XSSFReader(open);

            XSSFReader xssfReader = new XSSFReader(open);
            StylesTable styles = xssfReader.getStylesTable();

            InputStream firstSheet = reader.getSheetsData().next();
            processSheet(styles, sst, type, firstSheet);
            firstSheet.close();

            return poijiHandler.getDataset();
        } catch (SAXException | IOException | OpenXML4JException e) {
            throw new PoijiException("Problem occurred while reading data", e);
        }
    }

    private <T> void processSheet(StylesTable styles, ReadOnlySharedStringsTable readOnlySharedStringsTable,
                                  Class<T> type, InputStream sheetInputStream) throws IOException, SAXException {

        DataFormatter formatter = new DataFormatter();
        InputSource sheetSource = new InputSource(sheetInputStream);
        try {
            XMLReader sheetParser = SAXHelper.newXMLReader();
            poijiHandler = new PoijiHandler(readOnlySharedStringsTable, type, options);
            ContentHandler contentHandler =
                    new XSSFSheetXMLHandler(styles, null, readOnlySharedStringsTable, poijiHandler, formatter, false);
            sheetParser.setContentHandler(contentHandler);
            sheetParser.parse(sheetSource);
        } catch (ParserConfigurationException e) {
            throw new PoijiException("Problem occurred while reading data", e);
        }
    }
}
