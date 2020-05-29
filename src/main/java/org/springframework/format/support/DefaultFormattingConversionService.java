package org.springframework.format.support;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.CurrencyUnitFormatter;
import org.springframework.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.MonetaryAmountFormatter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

public class DefaultFormattingConversionService extends FormattingConversionService {
    private static final boolean jsr354Present;


    private static final boolean jodaTimePresent;

    static {
        ClassLoader classLoader = DefaultFormattingConversionService.class.getClassLoader();
        jsr354Present = ClassUtils.isPresent("javax.money.MonetaryAmount", classLoader);
        jodaTimePresent = ClassUtils.isPresent("org.joda.time.LocalDate", classLoader);
    }


    public DefaultFormattingConversionService() {
        this(null, true);
    }



    public DefaultFormattingConversionService(boolean registerDefaultFormatters) {
        this(null, registerDefaultFormatters);
    }


    public DefaultFormattingConversionService(
            @Nullable StringValueResolver embeddedValueResolver, boolean registerDefaultFormatters) {

        if (embeddedValueResolver != null) {
            setEmbeddedValueResolver(embeddedValueResolver);
        }
        DefaultConversionService.addDefaultConverters(this);
        if (registerDefaultFormatters) {
            addDefaultFormatters(this);
        }
    }

    public static void addDefaultFormatters(FormatterRegistry formatterRegistry) {
        // Default handling of number values
        formatterRegistry.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());

        // Default handling of monetary values
        if (jsr354Present) {
            formatterRegistry.addFormatter(new CurrencyUnitFormatter());
            formatterRegistry.addFormatter(new MonetaryAmountFormatter());
            formatterRegistry.addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
        }

        // Default handling of date-time values

        // just handling JSR-310 specific date and time types
        new DateTimeFormatterRegistrar().registerFormatters(formatterRegistry);

        if (jodaTimePresent) {
            // handles Joda-specific types as well as Date, Calendar, Long
            new JodaTimeFormatterRegistrar().registerFormatters(formatterRegistry);
        }
        else {
            // regular DateFormat-based Date, Calendar, Long converters
            new DateFormatterRegistrar().registerFormatters(formatterRegistry);
        }
    }











}
