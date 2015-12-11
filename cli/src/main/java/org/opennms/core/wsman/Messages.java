package org.opennms.core.wsman;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.kohsuke.args4j.Localizable;

public enum Messages implements Localizable {
    NO_ARGUMENT;

    @Override
    public String formatWithLocale(Locale locale, Object... args) {
        ResourceBundle localized = ResourceBundle.getBundle(Messages.class.getName(), locale);
        return MessageFormat.format(localized.getString(name()),args);
    }

    @Override
    public String format(Object... args) {
        return formatWithLocale(Locale.getDefault(),args);
    }
}
