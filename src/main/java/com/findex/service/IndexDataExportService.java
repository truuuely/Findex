package com.findex.service;

import com.findex.dto.indexdata.IndexDataQuery;
import com.findex.entity.IndexData;
import com.findex.enums.IndexDataExportHeader;
import com.findex.repository.indexdata.IndexDataRepository;
import com.opencsv.CSVWriter;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndexDataExportService {
    private static final int BATCH_SIZE = 1_000;

    private final IndexDataRepository indexDataRepository;
    private final EntityManager entityManager;

    public void generateCSV(IndexDataQuery indexDataQuery, Writer writer) {
        try (Stream<IndexData> stream = indexDataRepository.findAllForExport(indexDataQuery);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            csvWriter.writeNext(IndexDataExportHeader.getNames(), false);

            AtomicInteger counter = new AtomicInteger();
            stream.forEach(indexData -> {
                csvWriter.writeNext(new String[]{
                        indexData.getBaseDate().toString(),
                        toString(indexData.getMarketPrice()),
                        toString(indexData.getClosingPrice()),
                        toString(indexData.getHighPrice()),
                        toString(indexData.getLowPrice()),
                        toString(indexData.getVersus()),
                        toString(indexData.getFluctuationRate()),
                        String.valueOf(indexData.getTradingQuantity()),
                        String.valueOf(indexData.getTradingPrice()),
                        String.valueOf(indexData.getMarketTotalAmount())
                }, false);

                if (counter.incrementAndGet() % BATCH_SIZE == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed baseDateTo export CSV file");
        }
    }

    private String toString(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }
}
