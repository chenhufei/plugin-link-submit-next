package com.kunkunyu.link.submit.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.infra.ExternalUrlSupplier;

import java.net.URL;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CommonUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final ExternalUrlSupplier externalUrl;

    public String getDomain() {
        URL externalUrlRaw = externalUrl.getRaw();
        if (externalUrlRaw == null) {
            return null;
        }
        return externalUrlRaw.getAuthority();
    }

    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
