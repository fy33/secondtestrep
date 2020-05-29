package org.springframework.boot.autoconfigure.web.format;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.ClassUtils;

//todo
public class WebConversionService extends DefaultFormattingConversionService {
    private static final boolean JSR_354_PRESENT = ClassUtils.isPresent("javax.money.MonetaryAmount",
            WebConversionService.class.getClassLoader());

    @Deprecated
    private static final boolean JODA_TIME_PRESENT = ClassUtils.isPresent("org.joda.time.LocalDate",
            WebConversionService.class.getClassLoader());

    private static final Log logger = LogFactory.getLog(WebConversionService.class);



}
