/**
 * Copyright © MyCollab
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mycollab.reporting;

import com.mycollab.core.utils.BeanUtility;
import com.mycollab.db.arguments.SearchCriteria;
import com.mycollab.db.persistence.service.ISearchableService;
import net.sf.dynamicreports.report.exception.DRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author MyCollab Ltd
 * @since 5.1.4
 */
public abstract class SimpleReportTemplateExecutor<T> extends ReportTemplateExecutor {
    public static final String CRITERIA = "criteria";
    private static Logger LOG = LoggerFactory.getLogger(SimpleReportTemplateExecutor.class);

    protected AbstractReportBuilder reportBuilder;
    protected Class<T> classType;
    protected RpFieldsBuilder fieldBuilder;

    public SimpleReportTemplateExecutor(TimeZone timeZone, Locale locale, String reportTitle, RpFieldsBuilder fieldBuilder, ReportExportType outputForm, Class<T> classType) {
        super(timeZone, locale, reportTitle, outputForm);
        this.fieldBuilder = fieldBuilder;
        this.classType = classType;
    }

    @Override
    public void initReport() throws Exception {
        LOG.info("Export document: " + getOutputForm());
        if (getOutputForm() == ReportExportType.PDF) {
            reportBuilder = new PdfReportBuilder(getLocale(), fieldBuilder, classType, getParameters());
        } else if (getOutputForm() == ReportExportType.CSV) {
            reportBuilder = new CsvReportBuilder(getLocale(), fieldBuilder, classType, getParameters());
        } else if (getOutputForm() == ReportExportType.EXCEL) {
            reportBuilder = new XlsReportBuilder(getLocale(), fieldBuilder, classType, getParameters());
        } else {
            throw new IllegalArgumentException("Do not support output type " + getOutputForm());
        }
    }

    @Override
    public void outputReport(OutputStream outputStream) throws DRException, IOException {
        reportBuilder.toStream(outputStream);
    }

    public static class AllItems<S extends SearchCriteria, T> extends SimpleReportTemplateExecutor<T> {
        private ISearchableService<S> searchService;

        private int totalItems;

        public AllItems(TimeZone timeZone, Locale locale, String reportTitle, RpFieldsBuilder fieldBuilder, ReportExportType outputForm,
                        Class<T> classType, ISearchableService<S> searchService) {
            super(timeZone, locale, reportTitle, fieldBuilder, outputForm, classType);
            this.searchService = searchService;
        }

        @Override
        public void fillReport() {
            S searchCriteria = (S) getParameters().get(CRITERIA);
            totalItems = searchService.getTotalCount(searchCriteria);
            reportBuilder.setTitle(getReportTitle() + "(" + totalItems + ")");
            reportBuilder.setDataSource(new GroupIteratorDataSource(searchService, searchCriteria, totalItems));
            LOG.info(String.format("Fill report %d items and criteria %s", totalItems, BeanUtility.printBeanObj(searchCriteria)));
        }
    }

    public static class ListData<T> extends SimpleReportTemplateExecutor<T> {
        private List<T> data;

        public ListData(TimeZone timeZone, Locale locale, String reportTitle, RpFieldsBuilder fieldBuilder, ReportExportType outputForm, List<T> data, Class<T> classType) {
            super(timeZone, locale, reportTitle, fieldBuilder, outputForm, classType);
            this.data = data;
        }

        @Override
        public void fillReport() {
            BeanDataSource ds = new BeanDataSource(data);
            int totalItems = data.size();
            reportBuilder.setTitle(getReportTitle() + "(" + totalItems + ")");
            reportBuilder.setDataSource(ds);
            LOG.info(String.format("Fill report %d items", totalItems));
        }
    }
}
